#!/usr/bin/python
import web, json, restkit

elastic = restkit.Resource("http://localhost:9200/main")

class search(object):
    def GET(self):
        params = web.input()
        elasticResponse = json.loads(elastic.get(
            "doc/_search",
            payload=json.dumps({
                "query" : {
                    "filtered" : {
                        "query" : {"prefix" : {"text" : params['q']}},
                        "filter" : {"term" : {"source" : params['source']}}
                     }
                 },
                 "highlight" : {
                    "pre_tags" : ["<em>"],
                    "post_tags" : ["</em>"],
                    "fields" : {"text" : {"number_of_fragments" : 2}},
                    }
                 })).body_string())

        web.header("Content-Type", "application/json")
        return json.dumps(
            [dict(uri=hit['_id'], text=' '.join(hit['highlight']['text']),
                  source=hit['_source']['source'])
             for hit in elasticResponse['hits']['hits']])

class index(object):
    def GET(self):
        web.header("content-type", "application/xhtml+xml")
        return open("../webapp/index.html").read()
class jq(object):
    def GET(self):
        return open("../webapp/jquery-1.5.1.min.js").read()
app = web.application((
    r'/', 'index',
    r'/jquery-1.5.1.min.js', 'jq',
    r'/search', 'search',
    ), globals())
app.run()
