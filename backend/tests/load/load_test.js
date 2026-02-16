import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const errorRate = new Rate('errors');
const authDuration = new Trend('auth_duration');
const messageDuration = new Trend('message_duration');
const chatListDuration = new Trend('chat_list_duration');

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '2m', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '2m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    errors: ['rate<0.1'],
    auth_duration: ['p(95)<500'],
    message_duration: ['p(95)<500'],
  },
};

// Pre-generate phone numbers for VUs (up to 200 to cover all possible VU IDs)
const phones = new SharedArray('phones', function () {
  const arr = [];
  for (let i = 1; i <= 200; i++) {
    arr.push(`+1415555${String(i).padStart(4, '0')}`);
  }
  return arr;
});

// Cache tokens per VU to avoid re-authenticating on every iteration
const tokenCache = {};

function getToken(phone) {
  if (tokenCache[phone]) {
    return tokenCache[phone];
  }

  const start = Date.now();

  const otpRes = http.post(
    `${BASE_URL}/api/v1/auth/request-otp`,
    JSON.stringify({ phone }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'auth_request_otp' } }
  );

  if (otpRes.status !== 200) {
    errorRate.add(1);
    return null;
  }

  const otpBody = JSON.parse(otpRes.body);
  const otp = otpBody.data.otp;

  const verifyRes = http.post(
    `${BASE_URL}/api/v1/auth/verify-otp`,
    JSON.stringify({ phone, code: otp }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'auth_verify_otp' } }
  );

  if (verifyRes.status !== 200) {
    errorRate.add(1);
    return null;
  }

  authDuration.add(Date.now() - start);

  const data = JSON.parse(verifyRes.body).data;
  const result = {
    token: data.access_token,
    refresh: data.refresh_token,
  };

  // Get user ID from profile
  const profileRes = http.get(`${BASE_URL}/api/v1/users/me`, {
    headers: { Authorization: `Bearer ${result.token}`, 'Content-Type': 'application/json' },
    tags: { name: 'auth_get_profile' },
  });

  if (profileRes.status === 200) {
    const profile = JSON.parse(profileRes.body);
    result.userId = profile.data.id;
  }

  tokenCache[phone] = result;
  return result;
}

export default function () {
  const vuId = __VU;
  const iterationId = __ITER;
  const phone = phones[(vuId - 1) % phones.length];

  // Authenticate (cached after first call)
  const auth = getToken(phone);
  if (!auth) {
    sleep(2);
    return;
  }

  const authHeaders = {
    headers: {
      Authorization: `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    },
  };

  // 1. Get profile
  const profileRes = http.get(`${BASE_URL}/api/v1/users/me`, {
    ...authHeaders,
    tags: { name: 'get_profile' },
  });
  const profileOk = check(profileRes, {
    'profile 200': (r) => r.status === 200,
  });
  errorRate.add(!profileOk);

  sleep(0.5 + Math.random());

  // 2. List chats
  const chatStart = Date.now();
  const chatsRes = http.get(`${BASE_URL}/api/v1/chats`, {
    ...authHeaders,
    tags: { name: 'list_chats' },
  });
  const chatsOk = check(chatsRes, {
    'chats 200': (r) => r.status === 200,
  });
  chatListDuration.add(Date.now() - chatStart);
  errorRate.add(!chatsOk);

  sleep(0.5 + Math.random());

  // 3. Send a message (every 3rd iteration, only some VUs)
  if (iterationId % 3 === 0 && vuId % 3 === 0) {
    // Create a chat partner
    const partnerIdx = (vuId % phones.length) + 1;
    const partnerPhone = phones[partnerIdx % phones.length];
    const partnerAuth = getToken(partnerPhone);

    if (partnerAuth && partnerAuth.userId) {
      const chatRes = http.post(
        `${BASE_URL}/api/v1/chats`,
        JSON.stringify({ other_user_id: partnerAuth.userId }),
        { ...authHeaders, tags: { name: 'create_chat' } }
      );

      if (chatRes.status === 200 || chatRes.status === 201) {
        const chatBody = JSON.parse(chatRes.body);
        const chatId = chatBody.data.chat.id;

        const msgStart = Date.now();
        const msgRes = http.post(
          `${BASE_URL}/api/v1/messages`,
          JSON.stringify({
            chat_id: chatId,
            type: 'text',
            payload: { body: `Load test from VU ${vuId} iter ${iterationId}` },
            client_msg_id: `k6-${vuId}-${iterationId}-${Date.now()}`,
          }),
          { ...authHeaders, tags: { name: 'send_message' } }
        );

        const msgOk = check(msgRes, {
          'message sent': (r) => r.status === 200 || r.status === 201,
        });
        messageDuration.add(Date.now() - msgStart);
        errorRate.add(!msgOk);
      }
    }
  }

  sleep(1 + Math.random() * 2);
}
