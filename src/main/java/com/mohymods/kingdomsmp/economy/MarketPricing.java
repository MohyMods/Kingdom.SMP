package com.mohymods.kingdomsmp.economy;

/** Integer marginal prices. Splitting a batch cannot improve its total payout/cost. */
public final class MarketPricing {
    private MarketPricing() { }
    public static long unit(long minimum,long maximum,long used,long limit,boolean increasing){
        if(minimum<1||maximum<minimum||maximum>1_000_000L||limit<1||limit>1_000_000L||used<0||used>=limit)throw new IllegalArgumentException("Invalid market pricing bounds");
        long band=Math.min(3,used*4/limit),change=((maximum-minimum)*band+2)/3;
        return increasing?minimum+change:maximum-change;
    }
    public static long total(long minimum,long maximum,long used,long limit,int quantity,boolean increasing){
        if(quantity<1||quantity>2304||used<0||used>limit-quantity)throw new IllegalArgumentException("Insufficient market quota/stock");
        long total=0;for(int i=0;i<quantity;i++)total=Math.addExact(total,unit(minimum,maximum,used+i,limit,increasing));return total;
    }
    public static int affordable(long minimum,long maximum,long used,long limit,int requested,long coins,boolean increasing){
        if(minimum<1||maximum<minimum||maximum>1_000_000L||requested<1||requested>2304||coins<0||used<0||used>limit||limit<1||limit>1_000_000)throw new IllegalArgumentException("Invalid market request");
        int accepted=0;long sum=0;
        while(accepted<requested&&used+accepted<limit){long next=unit(minimum,maximum,used+accepted,limit,increasing);if(next>coins-sum)break;sum+=next;accepted++;}
        return accepted;
    }
}
