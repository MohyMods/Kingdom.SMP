package com.mohymods.kingdomsmp.client.screen;

import com.mohymods.kingdomsmp.client.CommerceController;
import com.mohymods.kingdomsmp.client.ui.*;
import com.mohymods.kingdomsmp.network.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.List;

/** Consistent Royal Gilded navigation for actual Architect stock, essentials and private mail. */
public final class CommerceScreen extends Screen {
    private static final String[] TABS={"Architect","Essentials","Mailbox","Bank","Progress"};
    private CommerceView view;
    private final Screen parent;
    private RoyalLayout box;
    private String selected="";
    private int quantity=64,page,rows,scroll,maxScroll;
    private int listX,listY,listW,listH,detailX,detailY,detailW,detailH;
    private boolean small,wide;
    private RoyalButton action,previous,next;
    private final List<RoyalButton> busyButtons=new ArrayList<>();

    public CommerceScreen(CommerceView view,Screen parent){super(text("Royal Exchange"));this.view=view;this.parent=parent;selectFirst();}
    public void accept(CommerceView update){
        // A permission/context failure can intentionally have no readable store revision.
        // Correlation already rejected obsolete replies; show the denial instead of retaining a stale review.
        if(update.revision()<view.revision()&&!(update.entries().isEmpty()&&!update.notice().isBlank()))return;
        boolean changedTab=update.tab()!=view.tab(),changedReview=update.reviewing()!=view.reviewing();
        view=update;
        if(changedTab){page=0;selected="";}
        if(changedTab||changedReview)scroll=0;
        selectFirst();rebuild();
    }
    public void invalidateReview(){
        view=new CommerceView(view.requestId(),view.revision(),view.open(),view.tab(),view.tier(),view.balance(),view.entries(),CommerceRequest.NONE,"",view.notice());
        scroll=0;rebuild();
    }
    private void selectFirst(){if(view.entries().stream().noneMatch(e->e.id().equals(selected)))selected=view.entries().isEmpty()?"":view.entries().getFirst().id();}
    private CommerceView.Entry selected(){return view.entries().stream().filter(e->e.id().equals(selected)).findFirst().orElse(null);}
    @Override protected void init(){
        busyButtons.clear();action=null;previous=null;next=null;small=width<360||height<220;
        if(small){addRenderableWidget(button(Math.max(0,width/2-40),height-26,80,"Close",b->onClose(),"Return to the kingdom."));return;}
        box=RoyalLayout.fit(width,height);int x=box.contentX(),w=box.contentWidth(),tw=(w-16)/5;
        for(int i=0;i<TABS.length;i++){
            int tab=i;busyButtons.add(addRenderableWidget(button(x+i*(tw+4),box.y()+48,tw,TABS[i],b->CommerceController.open(tab),"Read "+TABS[i].toLowerCase(Locale.ROOT)+" services.").selected(view.tab()==i)));
        }
        layoutBody();
        if(!view.reviewing()){
            rows=Math.max(1,(listH-24)/36);int pages=Math.max(1,(view.entries().size()+rows-1)/rows);page=Math.clamp(page,0,pages-1);
            for(int index=page*rows;index<Math.min(view.entries().size(),(page+1)*rows);index++){
                var entry=view.entries().get(index);int y=listY+(index-page*rows)*36;
                busyButtons.add(addRenderableWidget(button(listX+3,y+3,listW-6,entry.title(),b->{selected=entry.id();scroll=0;rebuild();},entry.detail()).selected(entry.id().equals(selected))));
            }
            int navW=Math.max(32,(listW-10)/2);
            previous=addRenderableWidget(button(listX+3,listY+listH-21,navW,"Previous",b->{page--;rebuild();},"Read earlier entries."));
            next=addRenderableWidget(button(listX+listW-navW-3,listY+listH-21,navW,"Next",b->{page++;rebuild();},"Read more entries."));
            if(wide&&(view.tab()==1||view.tab()==3||view.tab()==4)&&selected()!=null){
                int qw=(detailW-14)/4;int[] amounts={1,16,64,2304};
                for(int i=0;i<4;i++){
                    int amount=amounts[i];busyButtons.add(addRenderableWidget(button(detailX+4+i*(qw+2),detailY+detailH-24,qw,amount==2304?(view.tab()==3?"Max":"All"):Integer.toString(amount),b->{quantity=amount;rebuild();},"Review the accepted quantity before any items are consumed.").selected(quantity==amount)));
                }
            }
        }
        if(!wide&&!view.reviewing()&&(view.tab()==1||view.tab()==3||view.tab()==4)){
            int qw=w-266;
            busyButtons.add(addRenderableWidget(button(x+188,box.footerY(),qw,"Qty: "+(quantity==2304?(view.tab()==3?"Max":"All"):quantity),b->{quantity=switch(quantity){case 1->16;case 16->64;case 64->2304;default->1;};rebuild();},"Cycle 1, 16, 64 or a bounded maximum; review before committing.")));
        }
        action=addRenderableWidget(button(x,box.footerY(),108,actionLabel(),b->act(),"Review the exact server-validated transaction before committing."));
        addRenderableWidget(button(x+114,box.footerY(),68,"Refresh",b->CommerceController.open(view.tab()),"Inspect saved stock, balance and mailbox. Does not repeat a payment."));
        addRenderableWidget(button(box.x()+box.width()-86,box.footerY(),72,view.reviewing()?"Cancel":"Back",b->{if(view.reviewing())CommerceController.open(view.tab());else onClose();},"Return without another transaction."));
    }
    private void layoutBody(){
        int x=box.contentX(),y=box.bodyY(),w=box.contentWidth(),h=box.bodyHeight();wide=w>=430;
        if(wide){listX=x;listY=y;listW=Math.clamp(w/3,132,220);listH=h;detailX=x+listW+8;detailY=y;detailW=w-listW-8;detailH=h;}
        else{listX=x;listY=y;listW=w;listH=46;detailX=x;detailY=y+listH+6;detailW=w;detailH=Math.max(0,h-listH-6);}
    }
    private String actionLabel(){
        if(view.reviewing())return "Confirm";
        var row=selected();if(row==null)return "Review";
        return switch(row.state()){case "AVAILABLE"->"Review purchase";case "SELL"->"Review sale";case "BANK"->"Review transfer";case "CONTRIBUTE"->"Review donation";case "COINS"->"Review claim";case "BLUEPRINT"->"Manage plan";default->"Review";};
    }
    private void act(){
        if(view.reviewing()){CommerceController.request(2,view.tab(),selected,quantity,view.review());return;}
        var row=selected();if(row!=null)CommerceController.request(row.state().equals("BLUEPRINT")?3:1,view.tab(),row.id(),quantity,CommerceRequest.NONE);
    }
    private boolean actionable(){var row=selected();return row!=null&&Set.of("AVAILABLE","SELL","COINS","BLUEPRINT","BANK","CONTRIBUTE").contains(row.state());}
    private void rebuild(){if(minecraft!=null){clearWidgets();init();}}
    private RoyalButton button(int x,int y,int w,String label,net.minecraft.client.gui.components.Button.OnPress click,String hint){return new RoyalButton(x,y,w,20,text(label),click,text(hint));}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){ }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        g.fill(0,0,width,height,RoyalPalette.BACKDROP);
        if(small){g.drawCenteredString(font,"Increase the window or reduce GUI scale.",width/2,height/2,RoyalPalette.TEXT);super.render(g,mouseX,mouseY,partial);return;}
        RoyalPanel.frame(g,box.x(),box.y(),box.width(),box.height());
        line(g,"Royal Exchange · "+TABS[view.tab()],box.contentX(),box.y()+12,box.contentWidth(),RoyalPalette.GOLD);
        line(g,"Kingdom Tier "+view.tier(),box.contentX(),box.y()+29,112,RoyalPalette.TEXT);
        String balance=String.format(Locale.ROOT,"%,d coins",view.balance());int bw=Math.min(box.contentWidth()-116,font.width(balance));
        line(g,balance,box.contentX()+box.contentWidth()-bw,box.y()+29,bw,RoyalPalette.GOLD);
        if(view.reviewing())renderReview(g);else{renderRows(g);renderDetail(g);}
        boolean waiting=CommerceController.waiting();busyButtons.forEach(b->b.active=!waiting);
        if(action!=null)action.active=!waiting&&!CommerceController.refreshRequired()&&(view.reviewing()||actionable());
        if(previous!=null)previous.active=!waiting&&page>0;
        if(next!=null)next.active=!waiting&&(page+1)*rows<view.entries().size();
        String notice=CommerceController.status().isBlank()?view.notice():CommerceController.status();
        if(!notice.isBlank())line(g,notice,box.contentX(),box.footerY()-11,box.contentWidth(),RoyalPalette.MUTED);
        super.render(g,mouseX,mouseY,partial);
    }
    private void renderRows(GuiGraphics g){
        g.fill(listX,listY,listX+listW,listY+listH,RoyalPalette.PANEL);
        if(view.entries().isEmpty())line(g,view.tab()==2?"No deliveries yet.":"No entries available.",listX+6,listY+9,listW-12,RoyalPalette.MUTED);
        for(int i=page*rows;i<Math.min(view.entries().size(),(page+1)*rows);i++){
            var row=view.entries().get(i);int y=listY+(i-page*rows)*36;
            String label=stateLabel(row.state())+(row.price()>0?" · "+String.format(Locale.ROOT,"%,d",row.price())+" coins":"");
            if(wide)line(g,label,listX+7,y+25,listW-14,row.state().equals("LOCKED")?RoyalPalette.MUTED:RoyalPalette.GOLD);
        }
    }
    private void renderDetail(GuiGraphics g){
        if(detailH<=0)return;
        RoyalPanel.parchment(g,detailX,detailY,detailW,detailH);
        var row=selected();List<Component> text=new ArrayList<>();
        if(row==null){
            text.add(text(view.tab()==2?"YOUR KINGDOM MAILBOX":"ROYAL EXCHANGE"));
            text.add(text(view.tab()==2?"Unclaimed rewards and purchased plans appear here. Only your own deliveries are shown.":"Select an entry to inspect its current price, availability and requirements."));
        }else{
            text.add(text(row.title()));text.add(text(stateLabel(row.state())));text.add(text(row.detail()));
            if(row.price()>0)text.add(text((row.state().equals("SELL")?"Quoted unit value: ":"Price: ")+String.format(Locale.ROOT,"%,d",row.price())+" coins"));
            text.add(text(switch(row.state()){
                case "AVAILABLE"->"One primary blueprint per kingdom. The first successful buyer becomes its founder. Review before purchasing.";
                case "SELL"->"Choose an amount below. Only the exact reviewed items are consumed; budgets and allowances are checked again when you confirm.";
                case "COINS"->"Review the delivery to credit your available balance. A delivery can be claimed only once.";
                case "BLUEPRINT"->"Manage your authenticated plan through Construction. Recovering a lost unused plan invalidates older copies.";
                case "LOCKED"->"This entry is not available at the kingdom's current progression. No payment can be made.";
                case "SOLD"->"This kingdom's primary entitlement has already been purchased. Another purchase is not available.";
                case "CLAIMED"->"This delivery has already been claimed.";
                case "BANK"->"Use the actual Bank counter. Account, inventory and recipient are checked again when you confirm.";
                case "CONTRIBUTE"->"Shared Tier 2 milestone. Review before permanent resource consumption. Your contribution is recognized.";
                case "COMPLETE"->"This community objective is recorded as complete.";
                default->"Inspect this saved record or refresh for the latest state.";
            }));
        }
        if(!view.notice().isBlank())text.add(text(view.notice()));
        paragraphs(g,text,detailX+8,detailY+8,detailW-16,detailH-(wide&&(view.tab()==1||view.tab()==3||view.tab()==4)&&row!=null?40:16));
    }
    private void renderReview(GuiGraphics g){
        int x=box.contentX(),y=box.bodyY(),w=box.contentWidth(),h=box.bodyHeight();RoyalPanel.parchment(g,x,y,w,h);
        List<Component> lines=new ArrayList<>();lines.add(text("REVIEW YOUR TRANSACTION"));lines.add(text(view.reviewText()));
        lines.add(text("Confirm uses this exact review. If your inventory, balance or availability changes, request a new review."));
        if(!view.notice().isBlank())lines.add(text(view.notice()));paragraphs(g,lines,x+8,y+8,w-16,h-16);
    }
    private static String stateLabel(String state){return switch(state){case "AVAILABLE"->"Available";case "SELL"->"Sell to the kingdom";case "COINS"->"Coins to claim";case "BLUEPRINT"->"Your building plan";case "LOCKED"->"Locked";case "SOLD"->"Sold out";case "CLAIMED"->"Claimed";case "BANK"->"Bank service";case "CONTRIBUTE"->"Shared contribution";case "COMPLETE"->"Complete";default->state;};}
    private void paragraphs(GuiGraphics g,List<Component> values,int x,int y,int w,int h){
        if(w<=0||h<=0){maxScroll=0;return;}List<FormattedCharSequence> lines=new ArrayList<>();
        for(var value:values){lines.addAll(font.split(value,w-5));lines.add(FormattedCharSequence.EMPTY);}
        maxScroll=Math.max(0,lines.size()*12-h);scroll=Math.clamp(scroll,0,maxScroll);
        g.enableScissor(x,y,x+w,y+h);for(int i=0;i<lines.size();i++)g.drawString(font,lines.get(i),x,y+i*12-scroll,RoyalPalette.INK,false);g.disableScissor();
        if(maxScroll>0){int thumb=Math.max(6,h*h/(h+maxScroll)),top=y+(h-thumb)*scroll/maxScroll;g.fill(x+w-2,top,x+w,top+thumb,RoyalPalette.BRONZE);}
    }
    private void line(GuiGraphics g,String value,int x,int y,int w,int color){if(w>0)g.drawString(font,font.plainSubstrByWidth(value,w),x,y,color,false);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(!view.reviewing()&&x>=listX&&x<listX+listW&&y>=listY&&y<listY+listH){
            int pages=Math.max(1,(view.entries().size()+rows-1)/rows);page=Math.clamp(page-(int)Math.signum(vertical),0,pages-1);rebuild();return true;
        }
        scroll=Math.clamp(scroll-(int)(vertical*18),0,maxScroll);return true;
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==GLFW.GLFW_KEY_PAGE_UP||key==GLFW.GLFW_KEY_PAGE_DOWN){scroll=Math.clamp(scroll+(key==GLFW.GLFW_KEY_PAGE_DOWN?36:-36),0,maxScroll);return true;}
        return super.keyPressed(key,scan,mods);
    }
    @Override public void onClose(){
        CommerceController.close();minecraft.setScreen(parent);
        if(parent instanceof ConstructionScreen)com.mohymods.kingdomsmp.client.ConstructionController.open();
        else if(parent instanceof KingdomOverviewScreen)com.mohymods.kingdomsmp.client.ClientOverviewController.request();
    }
    @Override public boolean isPauseScreen(){return false;}
    private static Component text(String value){return Component.literal(value);}
}
