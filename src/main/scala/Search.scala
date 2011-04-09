import org.scalatra._
import dispatch._
import Http._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonParser._
import java.net.URLEncoder

class SearchApp extends ScalatraServlet {
  implicit val formats = net.liftweb.json.DefaultFormats

  val http = new Http
  val elastic = :/("localhost", 9200) / "main";
  // everything is in one type, with an ordinary source attribute to
  // separate the data sources
  val elasticTypeName = "doc"; 

  get("/") {
    <html>
      <head><title>search</title></head>
      <body>full-text search interface</body>
    </html>
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
    
    http.x(elastic / elasticTypeName / URLEncoder.encode(uri, "UTF-8") <<< 
	   compact(render(doc)) as_str)
  }

  get("/index") {
    // dump index, for testing 

    // todo: this is supposed to be a GET, but I think elastic is ok
    // with me sending the wrong method. I couldn't find how to force
    // the method name in dispatch.
    http.x(elastic / "_search" <<? Map("_pretty" -> "true") << compact(render(
      ("query" -> ("term" -> ( "source" -> "photo" )))
    )) as_str)
  }

  get("/search") {
    /*
     param source=s1,s2,s3, or omit to search everything
     param q=text, where text is to be found in the 'text' attr of the docs
     *
  * 
    */
    var elasticResponse = parse(http.x(elastic / "_search" << compact(render(
      ("query" -> 
       ("filtered" -> (
	 ("query" -> (
	   ("prefix" -> ("text" -> params("q")))
	 )) ~
	 ("filter" -> ("term" -> ("source" -> params("source"))))
       )
      )) ~ 
      ("highlight" -> (
        ("pre_tags" -> List("<em>")) ~
        ("post_tags" -> List("</em>")) ~
        ("fields" -> ("text" -> ("number_of_fragments" -> 2)))
      ))
      // would be nice to turn off _source, since those can be huge
    )) as_str))

    case class Highlight(text: List[String])
    case class Hit(_score: Int, _index: String, _id: String, _type: String, highlight: Highlight)
    case class Hits(hits: List[Hit])
    case class HitsResponse(hits: Hits)
    val extracted = elasticResponse.extract[HitsResponse]
    println(extracted)
    compact(render(
      ("hits" -> "h"
       
     )
    ))
  }
}
