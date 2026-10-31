package com.mohymods.kingdomsmp.client.screen;

import com.mohymods.kingdomsmp.client.ContributionController;
import com.mohymods.kingdomsmp.client.ui.*;
import com.mohymods.kingdomsmp.network.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** Royal Gilded donation review, authoritative ranking and personal accepted history. */
public final class ContributionScreen extends Screen {
    private static final String[] NAMES = {"Logs", "Cobblestone", "Wheat", "Iron ingots"};
    private static final int[] TARGETS = {1024, 2048, 512, 128};
    private static final Item[] ICONS = {Items.OAK_LOG, Items.COBBLESTONE, Items.WHEAT, Items.IRON_INGOT};
    private ContributionView view;
    private final Screen parent;
    private RoyalLayout box;
    private int tab, resource, quantity = 64, scroll, maxScroll;
    private boolean review, small;
    private RoyalButton action, older, newer;
    private final List<RoyalButton> mutationButtons = new ArrayList<>();
    public ContributionScreen(ContributionView view, Screen parent) { super(text("The First Hearth")); this.view = view; this.parent = parent; review = view.offerCount() > 0; }
    public void accept(ContributionView updated) {
        if (updated.revision() < view.revision()) return;
        if (review != (updated.offerCount() > 0)) scroll = 0;
        view = updated; review = updated.offerCount() > 0; rebuild();
    }
    public void invalidateReview() { review = false; rebuild(); }
    @Override protected void init() {
        mutationButtons.clear(); older=null;newer=null; small = width < 300 || height < 220;
        if (small) { addRenderableWidget(button(Math.max(0, width/2-40), height-26, 80, "Close", b -> onClose(), "Return to the kingdom.")); return; }
        box = RoyalLayout.fit(width, height);
        int x = box.contentX(), w = box.contentWidth();
        String[] tabs = {"Contribute", "Leaderboard", "Your history"}; int tw = (w - 8) / 3;
        for (int i = 0; i < 3; i++) { int selected = i; addRenderableWidget(button(x + i*(tw+4), box.y()+48, tw, tabs[i], b -> { tab=selected; review=false; scroll=0; rebuild(); }, "Inspect saved kingdom contributions.").selected(tab==i)); }
        if (tab == 0 && !review) {
            String[] shortNames = {"Logs", "Cobble", "Wheat", "Iron"}; int rw = (w-9)/4;
            for (int i=0;i<4;i++) { int selected=i; mutationButtons.add(addRenderableWidget(button(x+i*(rw+3), box.bodyY(), rw, shortNames[i], b -> { resource=selected; rebuild(); }, NAMES[i]+" — target "+TARGETS[i]).selected(resource==i))); }
            int[] amounts={1,16,64,0};
            for (int i=0;i<4;i++) { int amount=amounts[i]; mutationButtons.add(addRenderableWidget(button(x+i*(rw+3), box.bodyY()+76, rw, amount==0?"All":Integer.toString(amount), b -> { quantity=amount; rebuild(); }, "Review this quantity before any items are consumed.").selected(quantity==amount))); }
        }
        if (tab == 0) {
            action = addRenderableWidget(button(x, box.footerY(), 120, review ? "Confirm donation" : "Review donation", b -> {
                if (review) ContributionController.request(2, resource, 0, view.offer());
                else ContributionController.request(1, resource, quantity==0?TARGETS[resource]:quantity, ContributionRequest.NONE);
            }, review ? "Permanently consume exactly "+view.offerCount()+" accepted items." : "Get an exact server-validated donation quote."));
            mutationButtons.add(action);
        } else action = null;
        addRenderableWidget(button(x+(tab==0?126:0), box.footerY(), 68, "Refresh", b -> ContributionController.open(), "Read the saved result. Does not donate items."));
        if(tab==2){
            newer=addRenderableWidget(button(x+74,box.footerY(),58,"Newer",b->ContributionController.request(3,0,view.historyPage()-1,ContributionRequest.NONE),"Read newer accepted donations."));
            older=addRenderableWidget(button(x+136,box.footerY(),58,"Older",b->ContributionController.request(3,0,view.historyPage()+1,ContributionRequest.NONE),"Read older accepted donations."));
        }
        addRenderableWidget(button(box.x()+box.width()-86, box.footerY(),72,review?"Cancel":"Back",b -> { if(review){ review=false; rebuild(); }else onClose(); }, "Return without a donation."));
    }
    private RoyalButton button(int x,int y,int w,String label,net.minecraft.client.gui.components.Button.OnPress press,String help) { return new RoyalButton(x,y,w,20,text(label),press,text(help)); }
    private void rebuild() { if(minecraft!=null){clearWidgets();init();} }
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) { }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        g.fill(0,0,width,height,RoyalPalette.BACKDROP);
        if(small){g.drawCenteredString(font,"Increase the window or reduce GUI scale.",width/2,height/2,RoyalPalette.TEXT);super.render(g,mouseX,mouseY,partialTick);return;}
        RoyalPanel.frame(g,box.x(),box.y(),box.width(),box.height());
        g.drawString(font,title,box.contentX(),box.y()+12,RoyalPalette.GOLD,false);
        String state="Tier "+view.tier()+" · "+(view.complete()?"Milestone complete":"Shared community milestone");
        g.drawString(font,state,box.contentX(),box.y()+29,RoyalPalette.TEXT,false);
        if(box.width()>500)g.drawString(font,"Revision "+view.revision(),box.x()+box.width()-100,box.y()+29,RoyalPalette.MUTED,false);
        if(tab==0){if(review)renderReview(g);else renderContribution(g);} else renderList(g);
        boolean waiting=ContributionController.waiting();
        mutationButtons.forEach(b->b.active=!waiting);
        if(older!=null)older.active=!waiting&&view.historyPage()+1<view.historyPages();
        if(newer!=null)newer.active=!waiting&&view.historyPage()>0;
        if(action!=null) action.active=!waiting&&!ContributionController.needsRefresh()&&view.donationsOpen()
                &&(review || view.eligible().get(resource)>0&&view.totals().get(resource)<TARGETS[resource]);
        if(tab==0&&!notice().isBlank())line(g,notice(),box.contentX(),box.footerY()-12,box.contentWidth(),RoyalPalette.MUTED);
        super.render(g,mouseX,mouseY,partialTick);
    }
    private void renderContribution(GuiGraphics g) {
        int x=box.contentX(),y=box.bodyY(),w=box.contentWidth();
        RoyalPanel.parchment(g,x,y+26,w,box.bodyHeight()-26);
        RoyalProgress.render(g,x+7,y+31,w-14,7,view.totals().get(resource)/(double)TARGETS[resource]);
        line(g,NAMES[resource]+": "+view.totals().get(resource)+" / "+TARGETS[resource],x+7,y+44,w-14,RoyalPalette.INK);
        line(g,"You: "+view.personal().get(resource)+"   Inventory: "+view.eligible().get(resource),x+7,y+58,w-14,RoyalPalette.INK);
        if(box.bodyHeight()>145){
            g.renderItem(new ItemStack(ICONS[resource]),x+9,y+110);
            String selected="Review "+(quantity==0?"all eligible":quantity)+" "+NAMES[resource].toLowerCase(Locale.ROOT);
            line(g,selected,x+32,y+113,w-40,RoyalPalette.INK);
            List<Component> paragraphs=new ArrayList<>();
            paragraphs.add(text("Only accepted items are consumed. Excess stays in your inventory. Each resource category has equal score weight."));
            paragraphs.add(text("Your score: "+score(view.personal())+" / 4 · Rank: "+(view.rank()==0?"Unranked":view.rank())));
            paragraphs.add(text(view.complete()?"Highest contributor: "+view.winner()+". "+view.rewardTitle()+" privilege earned.":"Complete all four targets to reach Tier 2. Highest contributor earns the "+view.rewardTitle()+" construction privilege."));
            paragraphs.add(text(notice()));
            paragraphs(g,paragraphs,x+8,y+140,w-16,box.bodyHeight()-148);
        }else if(!notice().isBlank())line(g,notice(),x,box.footerY()-12,w,RoyalPalette.MUTED);
    }
    private void renderReview(GuiGraphics g) {
        int x=box.contentX(),y=box.bodyY(),w=box.contentWidth();
        RoyalPanel.parchment(g,x,y,w,box.bodyHeight());
        List<Component> lines=List.of(text("REVIEW CONTRIBUTION"),text("Donate "+view.offerCount()+" "+NAMES[view.offerResource()].toLowerCase(Locale.ROOT)),
                text("These accepted items will be consumed permanently. Excess stays with you."),text("Review expires after 30 seconds. Confirm revalidates your inventory and kingdom progress."),text(notice()));
        paragraphs(g,lines,x+8,y+8,w-16,box.bodyHeight()-16);
    }
    private void renderList(GuiGraphics g) {
        int x=box.contentX(),y=box.bodyY(),w=box.contentWidth();
        RoyalPanel.parchment(g,x,y,w,box.bodyHeight()); List<Component> lines=new ArrayList<>();
        if(tab==1){
            lines.add(text("HIGHEST CONTRIBUTORS · Equal weight per resource"));
            lines.add(text("Your rank: "+(view.rank()==0?"Unranked":view.rank())+" · Score "+score(view.personal())+" / 4"));
            if(view.leaders().isEmpty())lines.add(text("No donations yet. Light the first hearth together."));
            int rank=0;for(var row:view.leaders()){
                lines.add(text((++rank)+". "+row.name()+"  —  "+String.format(Locale.ROOT,"%.4f",row.score()/2048.0)+" points"));
                lines.add(text("Logs "+row.counts().get(0)+" · Stone "+row.counts().get(1)+" · Wheat "+row.counts().get(2)+" · Iron "+row.counts().get(3)));
            }
            lines.add(text("Ties: first to reach the tied score, then stable UUID order."));
            if(view.complete())lines.add(text("Permanent First Hearth winner: "+view.winner()+". Tier 2 reward: "+view.rewardTitle()+"."));
        }else{
            lines.add(text("YOUR ACCEPTED DONATIONS · Page "+(view.historyPage()+1)+" / "+view.historyPages()));
            if(view.history().isEmpty())lines.add(text("No accepted donations yet."));
            for(var row:view.history()){
                String time=DateTimeFormatter.ofPattern("dd MMM HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(row.time()));
                lines.add(text(time+"  +"+row.count()+" "+NAMES[row.resource()].toLowerCase(Locale.ROOT)));
            }
            lines.add(text("Personal totals: "+view.personal().get(0)+" logs · "+view.personal().get(1)+" cobblestone · "+view.personal().get(2)+" wheat · "+view.personal().get(3)+" iron."));
            lines.add(text("All accepted donations are retained in kingdom history."));
        }
        paragraphs(g,lines,x+8,y+8,w-16,box.bodyHeight()-16);
    }
    private String notice(){
        if(!ContributionController.status().isBlank())return ContributionController.status();
        if(!view.notice().isBlank())return view.notice();
        if(!view.donationsOpen()&&!view.complete())return "Survival/Adventure mode is required; recovery can also pause donations.";
        return "";
    }
    private void paragraphs(GuiGraphics g,List<Component> paragraphs,int x,int y,int w,int h){
        if(h<=0)return; List<FormattedCharSequence> lines=new ArrayList<>();for(var p:paragraphs){lines.addAll(font.split(p,w-5));lines.add(FormattedCharSequence.EMPTY);}
        maxScroll=Math.max(0,lines.size()*12-h);scroll=Math.clamp(scroll,0,maxScroll);
        g.enableScissor(x,y,x+w,y+h);for(int i=0;i<lines.size();i++)g.drawString(font,lines.get(i),x,y+i*12-scroll,RoyalPalette.INK,false);g.disableScissor();
        if(maxScroll>0){int thumb=Math.max(6,h*h/(h+maxScroll));int top=y+(h-thumb)*scroll/maxScroll;g.fill(x+w-2,top,x+w,top+thumb,RoyalPalette.BRONZE);}
    }
    private void line(GuiGraphics g,String s,int x,int y,int w,int color){g.drawString(font,font.plainSubstrByWidth(s,w),x,y,color,false);}
    private String score(List<Integer> p){return String.format(Locale.ROOT,"%.4f",(p.get(0)*2+p.get(1)+p.get(2)*4+p.get(3)*16)/2048.0);}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){scroll=Math.clamp(scroll-(int)(v*18),0,maxScroll);return true;}
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==GLFW.GLFW_KEY_PAGE_UP||key==GLFW.GLFW_KEY_PAGE_DOWN){scroll=Math.clamp(scroll+(key==GLFW.GLFW_KEY_PAGE_DOWN?36:-36),0,maxScroll);return true;}return super.keyPressed(key,scan,mods);}
    @Override public void onClose(){
        ContributionController.close();minecraft.setScreen(parent);
        if(parent instanceof KingdomOverviewScreen)com.mohymods.kingdomsmp.client.ClientOverviewController.request();
        if(parent instanceof ConstructionScreen)com.mohymods.kingdomsmp.client.ConstructionController.open();
    }
    @Override public boolean isPauseScreen(){return false;}
    private static Component text(String s){return Component.literal(s);}
}
