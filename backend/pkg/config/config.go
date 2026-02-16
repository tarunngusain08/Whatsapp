package config

import (
	"fmt"
	"os"
	"reflect"
	"strconv"
	"strings"
	"time"
)

func Load(cfg interface{}) error {
	v := reflect.ValueOf(cfg)
	if v.Kind() != reflect.Ptr || v.Elem().Kind() != reflect.Struct {
		return fmt.Errorf("config.Load: expected pointer to struct")
	}
	return loadStruct(v.Elem())
}

func loadStruct(v reflect.Value) error {
	t := v.Type()
	for i := 0; i < t.NumField(); i++ {
		field := t.Field(i)
		fv := v.Field(i)

		if field.Type.Kind() == reflect.Struct && field.Anonymous {
			if err := loadStruct(fv); err != nil {
				return err
			}
			continue
		}

		// Handle nested non-anonymous structs
		if field.Type.Kind() == reflect.Struct && field.Type != reflect.TypeOf(time.Duration(0)) {
			if err := loadStruct(fv); err != nil {
				return err
			}
			continue
		}

		envKey := field.Tag.Get("env")
		if envKey == "" {
			continue
		}

		val := os.Getenv(envKey)
		if val == "" {
			val = field.Tag.Get("envDefault")
		}
		if val == "" {
			if field.Tag.Get("envRequired") == "true" {
				return fmt.Errorf("config.Load: required env var %s not set", envKey)
			}
			continue
		}

		if err := setField(fv, val); err != nil {
			return fmt.Errorf("config.Load: field %s: %w", field.Name, err)
		}
	}
	return nil
}

func setField(fv reflect.Value, val string) error {
	switch fv.Kind() {
	case reflect.String:
		fv.SetString(val)
	case reflect.Int, reflect.Int64:
		if fv.Type() == reflect.TypeOf(time.Duration(0)) {
			d, err := time.ParseDuration(val)
			if err != nil {
				return err
			}
			fv.Set(reflect.ValueOf(d))
		} else {
			i, err := strconv.ParseInt(val, 10, 64)
			if err != nil {
				return err
			}
			fv.SetInt(i)
		}
	case reflect.Bool:
		b, err := strconv.ParseBool(val)
		if err != nil {
			return err
		}
		fv.SetBool(b)
	case reflect.Slice:
		if fv.Type().Elem().Kind() == reflect.String {
			parts := strings.Split(val, ",")
			fv.Set(reflect.ValueOf(parts))
		} else {
			return fmt.Errorf("unsupported slice type %s", fv.Type().Elem().Kind())
		}
	default:
		return fmt.Errorf("unsupported type %s", fv.Kind())
	}
	return nil
}
