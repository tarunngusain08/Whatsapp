import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const errorRate = new Rate('errors');
const authDuration = new Trend('auth_duration');
const messageDuration = new Trend('message_duration');
const chatListDuration = new Trend('chat_list_duration');

export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp up to 10 users
    { duration: '2m', target: 50 },    // Stay at 50 users
    { duration: '1m', target: 100 },   // Spike to 100 users
    { duration: '2m', target: 50 },    // Back to 50
    { duration: '30s', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    errors: ['rate<0.01'],
    auth_duration: ['p(95)<300'],
    message_duration: ['p(95)<500'],
  },
};

function getToken(phone) {
  const otpRes = http.post(`${BASE_URL}/api/v1/auth/request-otp`, JSON.stringify({ phone }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  if (otpRes.status !== 200) {
    errorRate.add(1);
    return null;
  }

  const otp = JSON.parse(otpRes.body).data.otp;
  
  const verifyRes = http.post(`${BASE_URL}/api/v1/auth/verify-otp`, JSON.stringify({ phone, code: otp }), {
    headers: { 'Content-Type': 'application/json' },
  });

  if (verifyRes.status !== 200) {
    errorRate.add(1);
    return null;
  }

  authDuration.add(otpRes.timings.duration + verifyRes.timings.duration);
  return JSON.parse(verifyRes.body).data.access_token;
}

export default function () {
  const vuId = __VU;
  const phone = `+1${String(vuId).padStart(10, '0')}`;

  // 1. Authenticate
  const token = getToken(phone);
  if (!token) {
    sleep(1);
    return;
  }

  const authHeaders = {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };

  // 2. Get profile
  const profileRes = http.get(`${BASE_URL}/api/v1/users/me`, authHeaders);
  check(profileRes, { 'profile 200': (r) => r.status === 200 });
  errorRate.add(profileRes.status !== 200);

  // 3. List chats
  const chatStart = Date.now();
  const chatsRes = http.get(`${BASE_URL}/api/v1/chats`, authHeaders);
  check(chatsRes, { 'chats 200': (r) => r.status === 200 });
  chatListDuration.add(Date.now() - chatStart);
  errorRate.add(chatsRes.status !== 200);

  // 4. Create a chat and send a message (only some VUs)
  if (vuId % 5 === 0) {
    const targetPhone = `+1${String(vuId + 1).padStart(10, '0')}`;
    const targetToken = getToken(targetPhone);
    if (targetToken) {
      // Get target user ID from token
      const parts = targetToken.split('.');
      const payload = JSON.parse(atob(parts[1]));
      const targetUserId = payload.user_id;

      const chatRes = http.post(`${BASE_URL}/api/v1/chats`, JSON.stringify({
        other_user_id: targetUserId,
      }), authHeaders);

      if (chatRes.status === 200 || chatRes.status === 201) {
        const chatId = JSON.parse(chatRes.body).data.chat.id;

        const msgStart = Date.now();
        const msgRes = http.post(`${BASE_URL}/api/v1/messages`, JSON.stringify({
          chat_id: chatId,
          type: 'text',
          payload: { body: `Load test message from VU ${vuId} at ${new Date().toISOString()}` },
          client_msg_id: `load-${vuId}-${Date.now()}`,
        }), authHeaders);

        check(msgRes, { 'message sent': (r) => r.status === 200 || r.status === 201 });
        messageDuration.add(Date.now() - msgStart);
        errorRate.add(msgRes.status !== 200 && msgRes.status !== 201);
      }
    }
  }

  sleep(1 + Math.random() * 2);
}

// base64 decode helper
function atob(str) {
  // k6 doesn't have atob, use encoding
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
  str = str.replace(/=+$/, '');
  // Add padding
  while (str.length % 4) str += '=';
  let output = '';
  for (let i = 0; i < str.length; i += 4) {
    const a = chars.indexOf(str[i]);
    const b = chars.indexOf(str[i + 1]);
    const c = chars.indexOf(str[i + 2]);
    const d = chars.indexOf(str[i + 3]);
    const bitmap = (a << 18) | (b << 12) | (c << 6) | d;
    output += String.fromCharCode((bitmap >> 16) & 255);
    if (str[i + 2] !== '=') output += String.fromCharCode((bitmap >> 8) & 255);
    if (str[i + 3] !== '=') output += String.fromCharCode(bitmap & 255);
  }
  return output;
}
