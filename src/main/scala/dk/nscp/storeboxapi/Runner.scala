package dk.nscp.storeboxapi

import scalaj.http._

import com.typesafe.config.ConfigFactory
import dk.nscp.storeboxapi.Structure._
import net.liftweb.json._

object Runner extends App {
  
  // Load config file "local.application.conf" if it exists, otherwise load "application.conf"
  val config = (new java.io.File("src/main/resources/local.application.conf").exists) match {
    case true => ConfigFactory.load("local.application.conf")
    case false => ConfigFactory.load()
  }

  // Get username and password from config file
  val username = config.getString("username")
  val password = config.getString("password")

  // Obtain an auth-token to use for requesting data
  val authURL = "https://www.storebox.com/api/v1/authenticate"
  val request = (Http(authURL)
    .method("POST")
    .postData(s"""{"username": "$username", "password":"$password"}""")
    .header("Accept", "application/json, text/plain, */*")
    .header("Content-Type", "application/json;charset=UTF-8")
  )

  val response = request.asString
  
  // Attempt to catch login failure:
  implicit val formats = DefaultFormats
  val code = (parse(response.body) \ "code").extract[Int]
  
  // code == 0 is login success!
  if (code == 0) {
    // Extract the auth token from the response cookie
    val cookies = response.headerSeq("Set-Cookie")
    val authCookie = (cookies.filter(cookieText => cookieText.startsWith("auth-token")).head.split(";")(0)).split("=")(1)

    // Prepare request to obtain a number of receipts for user
  	val amountToFetch = config.getInt("max_fetch_size")
  	val receiptsURL = s"https://www.storebox.com/api/v1/receipts?count=$amountToFetch"
    val receiptsRequest = (Http(receiptsURL)
  	  .method("GET")
  	  .header("Cookie", s"auth-token=$authCookie")
  	  .header("Content-Type", "application/json;charset=UTF-8")
	  )
	
  	val receiptsResponse = receiptsRequest.asString
  	if (receiptsResponse.code == 200) {
  	
  	  // Parse the json from the response and load into `Receipt` case class
  	  val json = parse(receiptsResponse.body)
  	  val receipts = (json \ "receipts").extract[List[Receipt]]

  	  // Get all the receipt IDs which we use to query each receipt and its details
  	  val receiptIds = receipts.map(receipt => receipt.receiptId)
      
  	  // flatMap over IDs and request receipt details for each receipt
  	  // and store this info in `ReceiptDetail` class.
  	  // Each `ReceiptDetail` contains all the entries of a receipt,
  	  // And we get these using the `asTsv` method which returns all
  	  // the entries of a recepit along with merchant info. `asTsv` returns a Seq
  	  // and this i flattened through flatmap resulting in one Seq of
  	  // receipt entries
  	  val receiptLines: Seq[String] = receiptIds.flatMap(id => {
  	    val detailsURL = s"https://www.storebox.com/api/v1/receipts/$id"
  	    val detailsRequest = (Http(detailsURL)
    	    .method("GET")
      		.header("Cookie", s"auth-token=$authCookie")
      		.header("Accept", "application/json, text/plain, */*")
      		.header("Content-Type", "application/json;charset=UTF-8")
    	  )
    	  val detailsResponse = detailsRequest.asString
    	  if (detailsResponse.code == 200) {
    	    val detailsJson = parse(detailsResponse.body)
  		    val detailsExtracted = detailsJson.extract[ReceiptDetail]
    	    // asTsv returns a Seq[String] - one line for each receipt line
    	    detailsExtracted.asTsv
    	  } else { 
    	    // Best error handling ever
    	    Seq(): Seq[String]
    	  }
  	  })
  	  
  	  // Write the contents to a file
  	  val writer = new java.io.BufferedWriter(new java.io.FileWriter(new java.io.File("output.tsv")))
  	  
  	  // Write headers first
  	  writer.write("date\tmerchant\taddress\tzipcode\tcity\titem\tdescription\tcount\tprice\n")
  	  
  	  // Write data second
  	  receiptLines.foreach(line => writer.write(line + "\n"))
  	  
  	  writer.close
  	  
  	} else {
  	  println(receiptsResponse.code)
  	}
  } else {
    println("Bad login")
  }
}
