package dk.nscp.storeboxapi

import scala.math.BigInt

object Structure {
  case class Receipt(
    receiptId: String,
    merchant: Merchant,
    purchaseDate: String,
    totalPrice: Price,
    summary: String
  )
  
  case class Price(
    amount: Float,
    vat: Float,
    currency: String
  )
  
  case class Merchant(
    merchantId: String,
    name: String,
    address: String,
    zipCode: String,
    city: String,
    phoneNumber: String,
    registrationNumber: String,
    logo: Option[String]
  )
  
  case class ReceiptLine(
    productNumber: Option[String],
    name: String,
    description: Option[String],
    count: Int,
    itemPrice: Price
  )
  
  case class Card(
    userId: String,
    cardId: String,
    name: String,
    truncatedCardNumber: String,
    cardTypeId: Int,
    expiry: String,
    multipleCardNumbers: Boolean,
    externalCardId: String,
    origin: String
  )
  
  case class Payment(
    `type`: String,
    price: Price,
    card: Card
  )
  
  case class Barcode(
    `type`: String,
    value: String,
    displayValue: String
  )
  
  case class ReceiptDetail(
    id: BigInt,
    `type`: String,
    receiptId: String,
    purchaseDate: String,
    orderNumber: String,
    price: Price,
    merchant: Merchant,
    receiptLines: Seq[ReceiptLine],
    payments: Seq[Payment],
    barcode: Barcode,
    footerText: Option[String],
    headerText: Option[String],
    userIds: Seq[String],
    imageId: Option[String]
  ) {
    def asTsv: Seq[String] = {
      receiptLines.map(line => {
        s"""$purchaseDate\t${merchant.name}\t${merchant.address}\t${merchant.zipCode}\t${merchant.city}\t${line.name}\t${line.description}\t${line.count}\t${line.itemPrice.amount}"""
      })
    }
  }
}