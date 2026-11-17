package com.mohymods.kingdomsmp.economy;
import java.util.*;
/** Adjacent physical denominations exchange at face value, never a configurable minting rate. */
public record CoinExchangeTerms(String source,String target,int batches){
 private static final List<String> DENOMINATIONS=List.of("copper","silver","gold","platinum");
 public CoinExchangeTerms{int from=DENOMINATIONS.indexOf(source),to=DENOMINATIONS.indexOf(target);if(from<0||to<0||Math.abs(from-to)!=1||batches<1||batches>230)throw new IllegalArgumentException("Exchange1–230batches between adjacent coin denominations");}
 public int inputs(){return batches*(DENOMINATIONS.indexOf(source)<DENOMINATIONS.indexOf(target)?10:1);}
 public int outputs(){return batches*(DENOMINATIONS.indexOf(source)<DENOMINATIONS.indexOf(target)?1:10);}
 public String action(){return "exchange:"+source+":"+target;}
 public static CoinExchangeTerms parse(String action,int batches){var parts=action.split(":",-1);if(parts.length!=3||!parts[0].equals("exchange"))throw new IllegalArgumentException("Choose a physical coin exchange");return new CoinExchangeTerms(parts[1],parts[2],batches);}
}
