package model

type RouteTarget struct {
	PathPrefix  string
	TargetURL   string
	StripPrefix bool
	RequireAuth bool
}
