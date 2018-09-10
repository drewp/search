package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/julienschmidt/httprouter"
	"strings"
	"log"
	"net/http"
	"strconv"
	"net/url"
	"io"
	"io/ioutil"
	//"reflect"
)


type SearchDoc struct {
	Source string `json:"source"`
	Uri    string `json:"uri"`
	Text   string `json:"text"`
	Title  string `json:"title"`
	View   string `json:"view"`
}

var elasticRoot = "http://bang:9200/main/"
// everything is in one type, with an ordinary source attribute to
// separate the data sources
var elasticTypeName = "doc"

func Root(w http.ResponseWriter, r *http.Request, _ httprouter.Params) {
	http.ServeFile(w, r, "../src/main/webapp/index.xhtml")
}

func SearchDocFromPostBody(body io.ReadCloser) (doc SearchDoc, err error) {
	decoder := json.NewDecoder(body)
	if err = decoder.Decode(&doc); err != nil {
		log.Printf("postbody err %+v", err)
		return doc, err
	}
	body.Close()
	return doc, nil
}

func EncodeToStream(doc SearchDoc, writer *io.PipeWriter) {
	defer writer.Close()
	enc := json.NewEncoder(writer)
	err := enc.Encode(doc)
	if err != nil {
		log.Fatal(err)
	}
}

func PostIndex(w http.ResponseWriter, r *http.Request, _ httprouter.Params) {
	source := r.URL.Query().Get("source")
	if source == "" {
		http.Error(w, "missing source", 400)
		return
	}
	
	doc, err := SearchDocFromPostBody(r.Body)
	if err != nil {
		http.Error(w, "postbody decode", 400)
		return
	}
	doc.Source = source
	
	//log.Printf("doc=%+v", doc)
	
	reader, writer := io.Pipe()
	go EncodeToStream(doc, writer)
	resp, err := http.Post(
		elasticRoot + elasticTypeName + "/" + url.PathEscape(doc.Uri),
		"application/json",
		reader)
	defer resp.Body.Close()
	if err != nil || (resp.StatusCode != 200 && resp.StatusCode != 201) {
		body, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			http.Error(w, "can't read elastic body", 500)
			return
		}
		http.Error(w, "elastic error: " + string(body), 500)
		return
	}

	fmt.Fprint(w, "ok\n")
}

func GetIndex(w http.ResponseWriter, r *http.Request, _ httprouter.Params) {
	fmt.Fprint(w, "Welcome!\n")
}


type ElasticQuery struct {
	From      int `json:"from"`
	Size      int `json:"size"`
	Highlight struct {
		PreTags  []string `json:"pre_tags"`
		PostTags []string `json:"post_tags"`
		Fields   struct {
			Text struct {
				NumberOfFragments int `json:"number_of_fragments"`
			} `json:"text"`
		} `json:"fields"`
		Encoder string `json:"encoder"`
	} `json:"highlight"`
	Aggs struct {
		Sources struct {
			Terms struct {
				Field string `json:"field"`
			} `json:"terms"`
		} `json:"sources"`
	} `json:"aggs"`
	Query struct {
		Bool struct {
			Filter struct {
				Terms struct {
					Source []string `json:"source"`
				} `json:"terms"`
			} `json:"filter"`
			Must struct {
				QueryString struct {
					Query           string `json:"query"`
					DefaultOperator string `json:"default_operator"`
					DefaultField    string `json:"default_field"`
				} `json:"query_string"`
			} `json:"must"`
		} `json:"bool"`
	} `json:"query"`
}

type ElasticResponse struct {
	Hits struct {
		MaxScore float64 `json:"max_score"`
		Total    int     `json:"total"`
		Hits     []struct {
			Highlight struct {
				Text []string `json:"text"`
			} `json:"highlight"`
			Type   string `json:"_type"`
			Source SearchDoc `json:"_source"`
			Id    string  `json:"_id"`
			Score float64 `json:"_score"`
			Index string  `json:"_index"`
		} `json:"hits"`
	} `json:"hits"`
	Shards struct {
		Successful int `json:"successful"`
		Total      int `json:"total"`
		Failed     int `json:"failed"`
	} `json:"_shards"`
	TimedOut     bool `json:"timed_out"`
	Aggregations struct {
		Sources struct {
			SumOtherDocCount int `json:"sum_other_doc_count"`
			Buckets          []struct {
				DocCount int    `json:"doc_count"`
				Key      string `json:"key"`
			} `json:"buckets"`
			DocCountErrorUpperBound int `json:"doc_count_error_upper_bound"`
		} `json:"sources"`
	} `json:"aggregations"`
	Took int `json:"took"`
}

type SearchResponseHit struct {
	Uri    string `json:"uri"`
	Title  string `json:"title"`
	Text   string `json:"text"`
	Source string `json:"source"`
	View   string `json:"view"`
}

type SearchResponse struct {
	Hits []SearchResponseHit `json:"hits"`
	SourceTotals map[string]interface{} `json:"sourceTotals"`
	TotalHits int `json:"totalHits"`
	Took      int `json:"took"`
}

func intParam(query url.Values, key string, missing int) (v int, err error) {
	s := query.Get(key)
	if s == "" {
		return missing, nil
	}
	i, err := strconv.Atoi(s)
	if err != nil {
		return 0, err
	}
	return i, nil
}

func makeElasticQuery(query url.Values) (eq ElasticQuery, err error) {
	from, err := intParam(query, "from", 0)
	if err != nil {
		return eq, err
	}
	eq.From = from
	
	size, err := intParam(query, "size", 10)
	if err != nil {
		return eq, err
	}
	eq.Size = size
	
	eq.Highlight.PreTags = []string{"<em>"}
	eq.Highlight.PostTags = []string{"</em>"}
	eq.Highlight.Fields.Text.NumberOfFragments = 2
	eq.Highlight.Encoder = "html"

	eq.Aggs.Sources.Terms.Field = "source"

	eq.Query.Bool.Filter.Terms.Source = query["source"]

	eq.Query.Bool.Must.QueryString.Query = query.Get("q")
	eq.Query.Bool.Must.QueryString.DefaultOperator = "OR"
	eq.Query.Bool.Must.QueryString.DefaultField = "text"
	return eq, nil
}

func makeElasticUrl(eq ElasticQuery) (outUrl string, err error) {
	jsonQuery, err := json.Marshal(eq)
	if err != nil {
		return "", err
	}
	log.Printf("q=%s", jsonQuery)

	params := url.Values{}
	params.Set("source_content_type", "application/json")
	params.Set("source", string(jsonQuery[:]))
	return elasticRoot + "_search?" + params.Encode(), nil
}

func makeSearchResponse(res ElasticResponse) SearchResponse {
	var sr SearchResponse
	sr.Took = res.Took
	sr.TotalHits = res.Hits.Total
	
	n := len(res.Hits.Hits)
	sr.Hits = make([]SearchResponseHit, n)
	for i, h := range res.Hits.Hits {
		sr.Hits[i].Uri = h.Source.Uri
		if h.Source.Title != "" {
			sr.Hits[i].Title = h.Source.Title
		} else {
			sr.Hits[i].Title = h.Id
		}
		sr.Hits[i].Text = strings.Join(h.Highlight.Text, " ")
		sr.Hits[i].Source = h.Source.Source
		sr.Hits[i].View = h.Source.View
	}

	sr.SourceTotals = make(map[string]interface{})
	for _, b := range res.Aggregations.Sources.Buckets {
		sr.SourceTotals[b.Key] = b.DocCount
	}
	
	return sr
}

func Search(w http.ResponseWriter, r *http.Request, _ httprouter.Params) {
	eq, err := makeElasticQuery(r.URL.Query())
	if err != nil {
		http.Error(w, "elastic query", 500)
		return
	}

	u, err := makeElasticUrl(eq)
	if err != nil {
		http.Error(w, "elastic url", 500)
		return
	}
	
	resp, err := http.Get(u)
	defer resp.Body.Close()
	if err != nil || resp.StatusCode != 200 {
		body, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			http.Error(w, "can't read elastic body", 500)
			return
		}
		http.Error(w, "elastic error: " + string(body), 500)
		
		return
	}
	returnedJson := new(bytes.Buffer)
	_, err = returnedJson.ReadFrom(resp.Body)
	if err != nil {
		http.Error(w, "elastic result read", 500)
		return
	}
	var res ElasticResponse
	err = json.Unmarshal(returnedJson.Bytes(), &res)
	if err != nil {
		http.Error(w, "elastic result parse", 500)
		return
	}
	resp.Body.Close()
	
	sr := makeSearchResponse(res)

	enc := json.NewEncoder(w)
	err = enc.Encode(sr)
	if err != nil {
		http.Error(w, "response encode", 500)
		return
	}
}

func main() {
	router := httprouter.New()
	router.GET("/", Root)
	router.POST("/index", PostIndex)
	router.GET("/index", GetIndex)
	router.GET("/search", Search)

	router.ServeFiles("/lib/*filepath",
		http.Dir("/my/proj/search/src/main/webapp/lib"))
	
	// need to make the index like this:
	// curl -XPUT http://bang:9200/main/ -d '{"settings":{"index":{"number_of_shards":1}},"mappings":{"doc":{"properties":{"source":{"type":"keyword"}}}}}'

	log.Print("serving")
	log.Fatal(http.ListenAndServe(":9096", router))
}
