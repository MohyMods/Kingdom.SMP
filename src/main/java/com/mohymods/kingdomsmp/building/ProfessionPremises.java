package com.mohymods.kingdomsmp.building;

import com.mohymods.kingdomsmp.profession.Profession;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Remaining approved premises gates. Vanilla workstations do not grant technology or signature equipment. */
public final class ProfessionPremises {
    public record Definition(Profession profession,String title,int tier,int price,String resident,String occupation,String greeting,List<String> stationIds){
        public Definition{stationIds=List.copyOf(stationIds);if(stationIds.size()!=4)throw new IllegalArgumentException("Four functional workstations required");}
        public List<Block> stations(){return stationIds.stream().map(id->{var key=ResourceLocation.parse(id);if(!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(key))throw new IllegalStateException("Missing native workstation "+id);return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(key);}).toList();}
        public ResourceLocation id(){return ResourceLocation.parse(profession.buildingId());}
        public String priceKey(){return "profession_"+profession.id();}
    }
    private static Definition d(Profession p,String title,int tier,String resident,String occupation,String greeting,String...stations){return new Definition(p,title,tier,tier==3?2000:tier==4?3000:tier==5?4500:6000,resident,occupation,greeting,List.of(stations));}
    private static final List<Definition> DEFINITIONS=List.of(
        d(Profession.BLACKSMITH,"Blacksmith's Forge",3,"Borin Emberforge","Blacksmith","Steel remembers careless work. Bring clean stock, and we will make tools worth trusting.","minecraft:blast_furnace","minecraft:anvil","minecraft:grindstone","minecraft:smithing_table"),
        d(Profession.MINER,"Miners' Guildhouse",3,"Doran Flint","Surveyor","Read the stone before you swing. A safe tunnel brings every miner home.","minecraft:furnace","minecraft:stonecutter","minecraft:cartography_table","minecraft:crafting_table"),
        d(Profession.MERCHANT,"Merchant's Counting House",3,"Selene Fairweather","Merchant","A fair measure earns a second customer. Every bargain here has a written account.","minecraft:cartography_table","minecraft:crafting_table","minecraft:barrel","minecraft:lectern"),
        d(Profession.ADVENTURER,"Adventurers' Guildhouse",3,"Kael Wayfarer","Expedition Guide","Bring a map, good provisions, and a story everyone returned to tell.","minecraft:cartography_table","minecraft:furnace","minecraft:grindstone","minecraft:crafting_table"),
        d(Profession.ENGINEER,"Engineer's Workshop",4,"Ivo Gearwright","Engineer","Measure the load before the motion. A clever machine must also be a safe one.","minecraft:smithing_table","minecraft:stonecutter","minecraft:crafting_table","minecraft:furnace"),
        d(Profession.MAGE,"Mage's Sanctum",4,"Lyra Starweave","Arcane Tutor","Wonder rewards patience. Learn the bounds of a spell before reaching beyond them.","minecraft:enchanting_table","minecraft:bookshelf","minecraft:crafting_table","minecraft:lectern"),
        d(Profession.LIBRARIAN,"Kingdom Library",4,"Orin Quill","Royal Librarian","A well-kept record outlasts a loud promise. Tell me what you wish to learn.","minecraft:lectern","minecraft:bookshelf","minecraft:cartography_table","minecraft:enchanting_table"),
        d(Profession.ALCHEMIST,"Alchemist's Laboratory",5,"Nessa Stillwater","Alchemist","Label every bottle, and trust no mixture you have not measured.","minecraft:brewing_stand","minecraft:cauldron","minecraft:crafting_table","minecraft:furnace"),
        d(Profession.MECHANIC,"Mechanic's Workshop",6,"Petra Coil","Mechanic","Power is useful only when you can stop it. Let us check the circuit together.","minecraft:smithing_table","minecraft:anvil","minecraft:blast_furnace","minecraft:crafting_table"));
    private static final Map<ResourceLocation,Definition> BY_ID;
    static{Map<ResourceLocation,Definition> rows=new HashMap<>();for(var d:DEFINITIONS)if(rows.put(d.id(),d)!=null)throw new IllegalStateException("Duplicate profession premises");BY_ID=Map.copyOf(rows);}
    private ProfessionPremises(){ }
    public static List<Definition> definitions(){return DEFINITIONS;}
    public static boolean contains(ResourceLocation id){return BY_ID.containsKey(id);}
    public static Definition get(ResourceLocation id){var d=BY_ID.get(id);if(d==null)throw new IllegalArgumentException("Unknown profession premises");return d;}
    public static ConstructionBlueprint create(ResourceLocation id){
        var d=get(id);boolean stone=d.profession()==Profession.BLACKSMITH||d.profession()==Profession.MINER||d.profession()==Profession.MECHANIC;
        var blocks=ProfessionBlueprints.shell(15,15,13,stone?Blocks.STONE_BRICKS.defaultBlockState():Blocks.SPRUCE_PLANKS.defaultBlockState(),Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),Blocks.DARK_OAK_PLANKS.defaultBlockState(),Blocks.DARK_OAK_STAIRS.defaultBlockState());
        ProfessionBlueprints.entrance(blocks,7);
        blocks.put(new BlockPos(4,1,3),Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        BlockPos[] stations={new BlockPos(3,1,7),new BlockPos(4,1,9),new BlockPos(10,1,7),new BlockPos(11,1,9)};for(int i=0;i<stations.length;i++){var state=d.stations().get(i).defaultBlockState();if(state.getBlock() instanceof GrindstoneBlock)state=state.setValue(BlockStateProperties.ATTACH_FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);blocks.put(stations[i],state);}
        for(int x:new int[]{2,3,10,11,12})blocks.put(new BlockPos(x,1,11),Blocks.BARREL.defaultBlockState());
        for(int x=10;x<=12;x++)blocks.put(new BlockPos(x,1,3),Blocks.BOOKSHELF.defaultBlockState());
        blocks.put(new BlockPos(3,1,4),Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST));
        blocks.put(new BlockPos(5,1,4),Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
        blocks.put(new BlockPos(4,1,4),Blocks.DARK_OAK_PLANKS.defaultBlockState());blocks.put(new BlockPos(4,2,4),Blocks.POTTED_FERN.defaultBlockState());
        ProfessionBlueprints.lantern(blocks,3,4,5);ProfessionBlueprints.lantern(blocks,11,4,5);ProfessionBlueprints.lantern(blocks,7,4,10);
        // The central three-block aisle, resident home and both door approaches remain unobstructed.
        return new ConstructionBlueprint(id,1,15,13,12,blocks.entrySet().stream().map(e->new BuilderHutBlueprint.Placement(e.getKey(),e.getValue())).toList(),new BlockPos(7,1,7),new BlockPos(7,1,7),new BlockPos(4,1,3));
    }
}
