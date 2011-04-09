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
        ret = []
        for hit in elasticResponse['hits']['hits']:
            s = hit['_source']
            ret.append(dict(uri=hit['_id'],
                            text=' '.join(hit['highlight']['text']),
                            source=s['source']))
            if 'view' in hit['_source']:
                ret[-1]['view'] = s['view']
                
        return json.dumps(ret)


class index(object):
    def GET(self):
        web.header("content-type", "application/xhtml+xml")
        return open("src/main/webapp/index.html").read()

app = web.application((
    r'/', 'index',
    r'/search', 'search',
    ), globals())
app.run()
