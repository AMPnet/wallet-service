package com.ampnet.walletservice.grpc.blockchain.pojo

import com.ampnet.crowdfunding.proto.ActiveSellOffersResponse

data class SellOfferData(
    val projectWalletHash: String,
    val sellerWalletHash: String,
    val shares: Long,
    val price: Long,
    val counterOffers: List<CounterOfferData>
) {
    constructor(offer: ActiveSellOffersResponse.SellOffer) : this(
        offer.projectTxHash,
        offer.sellerTxHash,
        offer.shares.toLong(),
        offer.price.toLong(),
        offer.counterOffersList.map { CounterOfferData(it) }
    )
}

data class CounterOfferData(
    val buyerWalletHash: String,
    val price: Long
) {
    constructor(offer: ActiveSellOffersResponse.CounterOffer) : this(
        offer.buyerTxHash,
        offer.price.toLong()
    )
}
