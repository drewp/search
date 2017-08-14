

v2/search: v2/src/search.go
	cd v2; /usr/lib/go-1.8/bin/go build src/search.go

deps:
	cd v2; go get github.com/julienschmidt/httprouter
