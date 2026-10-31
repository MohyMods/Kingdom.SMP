package com.mohymods.kingdomsmp.client.screen;

import com.mohymods.kingdomsmp.client.CommerceController;
import com.mohymods.kingdomsmp.client.ui.*;
import com.mohymods.kingdomsmp.network.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.List;

/** Consistent Royal Gilded navigation for actual Architect stock, essentials, private mail and auctions. */
public final class CommerceScreen extends Screen {
    private static final String[] TABS={"Architect","Supplies","Mailbox","Bank","Progress","Auctions"};
    private static final String[] COMPACT_TABS={"Plans","Supply","Mail","Bank","Tier","Trade"};
    private CommerceView view;
    private final Screen parent;
    private RoyalLayout box;
    private String selected="";
    private int quantity=64,page,rows,scroll,maxScroll;
    private int listX,listY,listW,listH,detailX,detailY,detailW,detailH;
    private boolean small,wide;
    private RoyalButton action,previous,next;
    private EditBox priceField;
    private String listingPrice="100";
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
        busyButtons.clear();action=null;previous=null;next=null;priceField=null;small=width<360||height<220;
        if(small){addRenderableWidget(button(Math.max(0,width/2-40),height-26,80,"Close",b->onClose(),"Return to the kingdom."));return;}
        box=RoyalLayout.fit(width,height);int x=box.contentX(),w=box.contentWidth(),tw=(w-4*(TABS.length-1))/TABS.length;
        for(int i=0;i<TABS.length;i++){
            int tab=i;String label=font.width(TABS[i])<=tw-8?TABS[i]:COMPACT_TABS[i];
            busyButtons.add(addRenderableWidget(button(x+i*(tw+4),box.y()+48,tw,label,b->CommerceController.open(tab),"Read "+TABS[i].toLowerCase(Locale.ROOT)+" services.").selected(view.tab()==i)));
        }
        layoutBody();
        if(!view.reviewing()&&listing()){
            // Keep price entry out of the parchment viewport at narrow GUI scales.
            priceField=addRenderableWidget(new EditBox(font,x+154,box.y()+25,Math.min(100,w-254),18,text("Total listing price in coins")));
            priceField.setMaxLength(10);
            priceField.setFilter(value->value.isEmpty()||value.chars().allMatch(c->c>='0'&&c<='9'));
            priceField.setValue(listingPrice);
            priceField.setResponder(value->listingPrice=value);
            priceField.setTooltip(Tooltip.create(text("Total price for the reviewed stack: 1 to 1,000,000,000 coins. This is not a price per item.")));
        }
        if(!view.reviewing()){
            rows=Math.max(1,(listH-24)/36);int pages=Math.max(1,(view.entries().size()+rows-1)/rows);page=Math.clamp(page,0,pages-1);
            for(int index=page*rows;index<Math.min(view.entries().size(),(page+1)*rows);index++){
                var entry=view.entries().get(index);int y=listY+(index-page*rows)*36;
                busyButtons.add(addRenderableWidget(button(listX+3,y+3,listW-6,entry.title(),b->{selected=entry.id();scroll=0;rebuild();},entry.detail()).selected(entry.id().equals(selected))));
            }
            int navW=Math.max(32,(listW-10)/2);
            previous=addRenderableWidget(button(listX+3,listY+listH-21,navW,"Previous",b->{page--;rebuild();},"Read earlier entries."));
            next=addRenderableWidget(button(listX+listW-navW-3,listY+listH-21,navW,"Next",b->{page++;rebuild();},"Read more entries."));
            if(wide&&wantsQuantity()&&selected()!=null){
                int qw=(detailW-14)/4;int[] amounts={1,16,64,2304};
                for(int i=0;i<4;i++){
                    int amount=amounts[i];busyButtons.add(addRenderableWidget(button(detailX+4+i*(qw+2),detailY+detailH-24,qw,amount==2304?(view.tab()==3?"Max":"All"):Integer.toString(amount),b->{quantity=amount;rebuild();},"Review the accepted quantity before any items are consumed.").selected(quantity==amount)));
                }
            }
        }
        if(!wide&&!view.reviewing()&&wantsQuantity()){
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
        return switch(row.state()){case "AVAILABLE"->"Review purchase";case "SELL"->"Review sale";case "BANK"->"Review transfer";case "CONTRIBUTE"->"Review donation";case "COINS","ITEMS"->"Review claim";case "LIST"->"Review listing";case "BUY"->"Review purchase";case "RETURN"->"Review return";case "BLUEPRINT"->"Manage plan";default->"Review";};
    }
    private void act(){
        if(view.reviewing()){CommerceController.request(2,view.tab(),selected,quantity,view.review());return;}
        var row=selected();if(row!=null)CommerceController.request(row.state().equals("BLUEPRINT")?3:1,view.tab(),row.id(),quantity,CommerceRequest.NONE,listing()?price():0);
    }
    private boolean listing(){var row=selected();return view.tab()==5&&row!=null&&row.state().equals("LIST");}
    private boolean wantsQuantity(){return view.tab()==1||view.tab()==3||view.tab()==4||listing();}
    private long price(){try{long value=Long.parseLong(listingPrice);return value>=1&&value<=1_000_000_000L?value:0;}catch(NumberFormatException invalid){return 0;}}
    private boolean actionable(){var row=selected();return row!=null&&Set.of("AVAILABLE","SELL","COINS","BLUEPRINT","BANK","CONTRIBUTE","LIST","BUY","RETURN","ITEMS").contains(row.state())&&(!listing()||price()>0);}
    private void rebuild(){if(minecraft!=null){clearWidgets();init();}}
    private RoyalButton button(int x,int y,int w,String label,net.minecraft.client.gui.components.Button.OnPress click,String hint){return new RoyalButton(x,y,w,20,text(label),click,text(hint));}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){ }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        g.fill(0,0,width,height,RoyalPalette.BACKDROP);
        if(small){g.drawCenteredString(font,"Increase the window or reduce GUI scale.",width/2,height/2,RoyalPalette.TEXT);super.render(g,mouseX,mouseY,partial);return;}
        RoyalPanel.frame(g,box.x(),box.y(),box.width(),box.height());
        boolean editingListing=!view.reviewing()&&listing();
        String balance=String.format(Locale.ROOT,"%,d coins",view.balance());
        int bw=Math.min(box.contentWidth()-(editingListing?174:116),font.width(balance));
        line(g,"Royal Exchange · "+TABS[view.tab()],box.contentX(),box.y()+12,editingListing?box.contentWidth()-bw-8:box.contentWidth(),RoyalPalette.GOLD);
        line(g,"Kingdom Tier "+view.tier(),box.contentX(),box.y()+29,112,RoyalPalette.TEXT);
        line(g,balance,box.contentX()+box.contentWidth()-bw,box.y()+(editingListing?12:29),bw,RoyalPalette.GOLD);
        if(editingListing)line(g,"Price",box.contentX()+118,box.y()+29,34,price()>0?RoyalPalette.GOLD:RoyalPalette.MUTED);
        if(view.reviewing())renderReview(g);else{renderRows(g);renderDetail(g);}
        boolean waiting=CommerceController.waiting();busyButtons.forEach(b->b.active=!waiting);
        if(priceField!=null)priceField.setEditable(!waiting&&!CommerceController.refreshRequired());
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
                case "ITEMS"->"Review the exact saved item delivery. Make enough main-inventory space; items are never dropped. A delivery can be claimed once.";
                case "LIST"->"Hold the stack in your main hand, choose a quantity and enter its TOTAL price above. Confirm moves the exact items into escrow for a fixed-price listing. Ordinary sales pay 5% tax, rounded up. Timed bidding is not available in this build.";
                case "BUY"->"Review this fixed-price listing before paying. A successful purchase reserves its items in your private Mailbox. Competing buyers cannot purchase the same listing.";
                case "RETURN"->"Review returning these escrowed items to your private Mailbox. A purchased listing cannot be reclaimed. No second item reward is created.";
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
        paragraphs(g,text,detailX+8,detailY+8,detailW-16,detailH-(wide&&wantsQuantity()&&row!=null?40:16));
    }
    private void renderReview(GuiGraphics g){
        int x=box.contentX(),y=box.bodyY(),w=box.contentWidth(),h=box.bodyHeight();RoyalPanel.parchment(g,x,y,w,h);
        List<Component> lines=new ArrayList<>();lines.add(text("REVIEW YOUR TRANSACTION"));lines.add(text(view.reviewText()));
        lines.add(text("Confirm uses this exact review. If your inventory, balance or availability changes, request a new review."));
        if(!view.notice().isBlank())lines.add(text(view.notice()));paragraphs(g,lines,x+8,y+8,w-16,h-16);
    }
    private static String stateLabel(String state){return switch(state){case "AVAILABLE"->"Available";case "SELL"->"Sell to the kingdom";case "COINS"->"Coins to claim";case "ITEMS"->"Items to claim";case "LIST"->"List held items";case "BUY"->"Fixed-price listing";case "RETURN"->"Return escrow";case "BLUEPRINT"->"Your building plan";case "LOCKED"->"Locked";case "SOLD"->"Sold out";case "CLAIMED"->"Claimed";case "BANK"->"Bank service";case "CONTRIBUTE"->"Shared contribution";case "COMPLETE"->"Complete";default->state;};}
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
