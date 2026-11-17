package com.mohymods.kingdomsmp.integration.space;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Approved destinations: first space stage10, advanced planets11; native rocket rules still apply. */
public final class SpaceTravelRules {
    public record Destination(String dimension,String name,int tier) { }
    public static final int MINIMUM_TIER=10;
    private static final List<Destination> REALMS=List.of(
        new Destination("ad_astra:earth_orbit","Earth Orbit",10),new Destination("ad_astra:moon","The Moon",10),new Destination("ad_astra:moon_orbit","Moon Orbit",10),
        new Destination("ad_astra:mars","Mars",11),new Destination("ad_astra:mars_orbit","Mars Orbit",11),new Destination("ad_astra:venus","Venus",11),new Destination("ad_astra:venus_orbit","Venus Orbit",11),
        new Destination("ad_astra:mercury","Mercury",11),new Destination("ad_astra:mercury_orbit","Mercury Orbit",11),new Destination("ad_astra:glacio","Glacio",11),new Destination("ad_astra:glacio_orbit","Glacio Orbit",11));
    private static final Set<String> IDS=REALMS.stream().map(Destination::dimension).collect(Collectors.toUnmodifiableSet());
    private SpaceTravelRules() { }
    public static List<Destination> realms(){return REALMS;}
    public static Set<String> destinations(){return IDS;}
    public static boolean space(String dimension){return IDS.contains(dimension);}
    public static int requiredTier(String dimension){return REALMS.stream().filter(r->r.dimension().equals(dimension)).mapToInt(Destination::tier).findFirst().orElse(1);}
    public static String name(String dimension){return REALMS.stream().filter(r->r.dimension().equals(dimension)).map(Destination::name).findFirst().orElse(dimension);}
}
