import org.scalatra._
import dispatch._
import Http._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonParser._
import net.liftweb.json.Merge
import java.net.URLEncoder


case class Highlight(text: List[String])
case class Source(source: String, view: Option[String])
case class Hit(_score: Double, _index: String, _id: String, _type: String, 
	       _source: Source, highlight: Highlight)
case class Hits(hits: List[Hit], total: Int)
case class FacetTerm(count: Int, term: String)
case class FacetsSource(terms: List[FacetTerm])
case class Facets(source: FacetsSource)
case class HitsResponse(hits: Hits, facets: Facets, took: Int)

class SearchApp extends ScalatraServlet {
  implicit val formats = net.liftweb.json.DefaultFormats

  val elastic = :/("bang", 9200) / "main";
  // everything is in one type, with an ordinary source attribute to
  // separate the data sources
  val elasticTypeName = "doc"; 

  before {
    contentType = "application/json"
  }

  post("/index") {
    /*
     param source=<data source name>
     body is a json doc with uri/text and optional title and modified

     Existing doc with the same uri will be overwritten. source is a
     url param, not a doc attribute, in case someday I want to use it
     to pick elastic type or index instead
     */
    val doc = parse(request.body) merge ("source" -> params("source"))
    val uri = (doc \ "uri").children(0).asInstanceOf[JString].s
    
    val http = new Http
    http.x(elastic / elasticTypeName / URLEncoder.encode(uri, "UTF-8") <<< 
	   compact(render(doc)) as_str)
  }

  get("/index") {
    // dump index, for testing 

    // todo: this is supposed to be a GET, but I think elastic is ok
    // with me sending the wrong method. I couldn't find how to force
    // the method name in dispatch.
    val http = new Http
    http.x(elastic / "_search" <<? Map("_pretty" -> "true") << compact(render(
      ("query" -> ("term" -> ( "source" -> params("source") )))
    )) as_str)
  }

  get("/search") {
    /*
     param source=s1,s2,s3, or omit to search everything
     param q=text, where text is to be found in the 'text' attr of the docs
    */
    val http = new Http

    val jsonQuery = compact(render(
      ("query" -> ("query_string" -> (
	// this analyzer, whatever it is, allows the user to search on
	// other fields, which may be a security hole once I add the
	// security stuff.
	("query" -> params("q")) ~
	("default_field" -> "text") ~
	("default_operator" -> "OR")
      ))) ~
      // a filter out here is only on results; it doesn't affect facet counts
      ("from" -> params.getOrElse("from", "0").toInt) ~
      ("size" -> params.getOrElse("size", "10").toInt) ~
      ("filter" -> ("terms" -> ("source" -> multiParams("source")))) ~ 
      ("highlight" -> (
        ("pre_tags" -> List("<em>")) ~
        ("post_tags" -> List("</em>")) ~
        ("fields" -> ("text" -> ("number_of_fragments" -> 2)))
      )) ~
      ("facets" -> ("source" -> ("terms" -> ("field" -> "source"))))
    ))
    println(jsonQuery)
    val elasticResponse = parse(
      http.x(elastic / "_search" <<? Map("source" -> jsonQuery) as_str)
    )
    // errors from elastic are just falling right through, currently
    val extracted = elasticResponse.extract[HitsResponse]
  
    compact(render(
      ("hits" -> 
       extracted.hits.hits.map(h => Merge.merge(
	 ("uri" -> h._id) ~
	 ("text" -> h.highlight.text.mkString(" ")) ~
	 ("source" -> h._source.source),
	 h._source.view match {
	   case None => JNothing
	   case Some(view) => ("view" -> view)
	 }))) ~
      ("sourceTotals" -> 
       JObject(extracted.facets.source.terms.map(
	 t => JField(t.term, t.count)))) ~
      ("totalHits" -> extracted.hits.total) ~
      ("took" -> extracted.took)
    ))
  }
}
