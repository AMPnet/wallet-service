package com.ampnet.walletservice.grpc.blockchain.pojo

import com.ampnet.crowdfunding.proto.SellOffer

data class SellOfferData(
    val projectWalletHash: String,
    val sellerWalletHash: String,
    val shares: Long,
    val price: Long
) {
    constructor(offer: SellOffer) : this(
        offer.projectTxHash,
        offer.sellerTxHash,
        offer.shares.toLong(),
        offer.price.toLong()
    )
}
