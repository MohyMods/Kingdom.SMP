package com.mohymods.kingdomsmp.client.screen;

import com.mohymods.kingdomsmp.client.*;
import com.mohymods.kingdomsmp.client.ui.*;
import com.mohymods.kingdomsmp.network.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.List;

/** Blueprint, actual project, Reeve services and permissioned construction administration. */
public final class ConstructionScreen extends Screen {
    private ConstructionView view;
    private final Screen parent;
    private RoyalLayout box;
    private int tab,rotation,scroll,maxScroll;
    private boolean small;
    private EditBox xField,yField,zField;
    private String xValue,yValue,zValue;
    private final List<RoyalButton> mutations=new ArrayList<>();
    private String localNotice="";
    private final String greeting;
    public ConstructionScreen(ConstructionView view,Screen parent){
        super(text(residentGreeting(view.notice())?view.resident()+" · "+(view.modern()?"Royal Steward":"Reeve"):view.title()));
        this.view=view;this.parent=parent;rotation=view.rotation();
        greeting=residentGreeting(view.notice())?view.notice():"";
        if(!greeting.isBlank()||view.notice().startsWith("Community milestone board"))tab=1;
    }
    private static boolean residentGreeting(String notice){return notice.contains(" — Reeve:")||notice.contains(" — Royal Steward:");}
    public void accept(ConstructionView updated){
        if(updated.revision()<view.revision())return;
        boolean rebuild=updated.requestId()>0||!updated.phase().equals(view.phase())||updated.reviewAction()!=view.reviewAction()||updated.admin()!=view.admin()||!updated.blueprint().equals(view.blueprint());
        if(rebuild)remember();
        if(!updated.blueprint().equals(view.blueprint())){xValue=null;yValue=null;zValue=null;tab=0;}
        view=updated;
        if(updated.requestId()>0&&updated.hasSite()){rotation=updated.rotation();xValue=Integer.toString(updated.origin().getX());yValue=Integer.toString(updated.origin().getY());zValue=Integer.toString(updated.origin().getZ());rebuild=true;}
        if(rebuild){scroll=0;clearWidgets();init();}
    }
    private void remember(){if(xField!=null){xValue=xField.getValue();yValue=yField.getValue();zValue=zField.getValue();}}
    private void rebuild(){remember();clearWidgets();init();}
    @Override protected void init(){
        mutations.clear();xField=null;yField=null;zField=null;small=width<300||height<220;
        if(small){addRenderableWidget(button(Math.max(0,width/2-40),height-26,80,"Close",b->onClose(),"Return to the kingdom."));return;}
        box=RoyalLayout.fit(width,height);int x=box.contentX(),w=box.contentWidth();
        int tabs=view.admin()?3:2,tw=(w-(tabs-1)*4)/tabs;
        String[] names={"Blueprint","Project","Royal Admin"};
        for(int i=0;i<tabs;i++){int selected=i;addRenderableWidget(button(x+i*(tw+4),box.y()+48,tw,names[i],b->{remember();tab=selected;scroll=0;rebuild();},"Inspect "+names[i].toLowerCase(Locale.ROOT)+".").selected(tab==i));}
        if(view.reviewAction()==0){
            if(tab==0){
                BlockPos initial=view.hasSite()?view.origin():minecraft.player.blockPosition();
                if(xValue==null){xValue=Integer.toString(initial.getX());yValue=Integer.toString(initial.getY());zValue=Integer.toString(initial.getZ());}
                int fw=(w-8)/3,y=box.bodyY()+12;
                xField=field(x,y,fw,"Foundation X",xValue);yField=field(x+fw+4,y,fw,"Foundation Y",yValue);zField=field(x+2*(fw+4),y,fw,"Foundation Z",zValue);
                boolean editable=view.owner()&&view.hasPlan()&&!view.phase().equals("BUILDING")&&!view.phase().equals("COMPLETE");
                xField.setEditable(editable);yField.setEditable(editable);zField.setEditable(editable);
                int bw=(w-8)/3;
                mutations.add(addRenderableWidget(button(x,box.bodyY()+38,bw,rotationName(),b->{rotation=(rotation+1)%4;rebuild();},"Rotate the proposed footprint clockwise. Dimensions are revalidated.")));
                mutations.add(addRenderableWidget(button(x+bw+4,box.bodyY()+38,bw,"Site here",b->{BlockPos p=minecraft.player.blockPosition();xField.setValue(Integer.toString(p.getX()));yField.setValue(Integer.toString(p.getY()));zField.setValue(Integer.toString(p.getZ()));remember();},"Use your standing position as the minimum foundation corner; prepare clear supported ground.")));
                RoyalButton preview=addRenderableWidget(button(x+2*(bw+4),box.bodyY()+38,bw,"Preview",b->{try{ConstructionController.request(3,coordinates(),rotation,ConstructionRequest.NONE);localNotice="";}catch(IllegalArgumentException error){localNotice="Enter whole foundation coordinates within the world limits.";}},"Validate and outline the whole site before submitting it to an administrator."));
                preview.active=editable;mutations.add(preview);
                if(!editable)mutations.forEach(control->control.active=false);
                RoyalButton claim=addRenderableWidget(button(x,box.bodyY()+64,Math.min(180,w),view.phase().equals("AVAILABLE")?"Claim blueprint":"Recover blueprint",b->send(1),"Review recovery: all previous copies and approvals will be invalidated."));
                claim.active=view.owner()&&!view.phase().equals("BUILDING")&&!view.phase().equals("COMPLETE");mutations.add(claim);
            }else if(tab==1){
                boolean commerce=view.phase().equals("COMPLETE")&&!view.blueprint().equals(com.mohymods.kingdomsmp.building.BlueprintRegistry.HUT);
                int bw=(w-(commerce?8:4))/(commerce?3:2);
                addRenderableWidget(button(x,box.bodyY()+28,bw,"Community milestone",b->ContributionController.open(),"Inspect or donate through the actual First Hearth contribution service."));
                addRenderableWidget(button(x+bw+4,box.bodyY()+28,bw,"Kingdom overview",b->ClientOverviewController.request(),"Read current kingdom progression. No service unlock is granted."));
                if(commerce)addRenderableWidget(button(x+2*(bw+4),box.bodyY()+28,bw,"Royal services",b->com.mohymods.kingdomsmp.client.CommerceController.open(0),"Blueprint market, Essentials and your private mailbox. Visit the resident to trade."));
                if(view.phase().equals("APPROVED")&&view.owner())mutations.add(addRenderableWidget(button(x,box.bodyY()+52,Math.min(180,w),"Review construction",b->send(7),"Review the one-use plan redemption at the approved location.")));
            }else{
                RoyalButton approve=addRenderableWidget(button(x,box.bodyY(),Math.min(180,w),"Review site approval",b->send(5),"Inspect the exact proposed footprint nearby; confirm separately. No terrain changes."));
                approve.active=view.phase().equals("PROPOSED");mutations.add(approve);
                RoyalButton resume=addRenderableWidget(button(x,box.bodyY()+24,Math.min(180,w),"Inspect / resume",b->send(9),"Verify and resume only the same existing project; no missing material or NPC regeneration."));
                resume.active=view.phase().equals("BUILDING");mutations.add(resume);
            }
        }else{
            mutations.add(addRenderableWidget(button(x,box.footerY(),92,"Confirm",b->ConstructionController.request(view.reviewAction(),BlockPos.ZERO,0,view.review()),"Carry out exactly the action shown in this review.")));
        }
        addRenderableWidget(button(x+(view.reviewAction()>0?98:0),box.footerY(),64,"Refresh",b->ConstructionController.open(),"Cancel a pending review and read the actual saved record without a mutation."));
        addRenderableWidget(button(box.x()+box.width()-86,box.footerY(),72,view.reviewAction()>0?"Cancel":"Back",b->{if(view.reviewAction()>0)ConstructionController.open();else onClose();},"Leave this review without committing."));
    }
    private EditBox field(int x,int y,int w,String label,String value){
        EditBox field=new EditBox(font,x,y,w,20,text(label));field.setMaxLength(10);field.setValue(value);addRenderableWidget(field);return field;
    }
    private BlockPos coordinates(){
        BlockPos p=new BlockPos(Integer.parseInt(xField.getValue()),Integer.parseInt(yField.getValue()),Integer.parseInt(zField.getValue()));
        if(Math.abs((long)p.getX())>30_000_000||Math.abs((long)p.getZ())>30_000_000||p.getY()<-2048||p.getY()>2048)throw new IllegalArgumentException("Outside world limits");
        return p;
    }
    private void send(int action){ConstructionController.request(action,BlockPos.ZERO,0,ConstructionRequest.NONE);}
    private RoyalButton button(int x,int y,int w,String label,net.minecraft.client.gui.components.Button.OnPress press,String help){return new RoyalButton(x,y,w,20,text(label),press,text(help));}
    private String rotationName(){return new String[]{"North","East","South","West"}[rotation];}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float t){ }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,RoyalPalette.BACKDROP);
        if(small){g.drawCenteredString(font,"Increase the window or reduce GUI scale.",width/2,height/2,RoyalPalette.TEXT);super.render(g,mx,my,partial);return;}
        RoyalPanel.frame(g,box.x(),box.y(),box.width(),box.height());
        g.drawString(font,greeting.isBlank()?text(view.title()):title,box.contentX(),box.y()+12,RoyalPalette.GOLD,false);
        String state=view.phase().replace('_',' ')+" · "+view.winner();
        g.drawString(font,font.plainSubstrByWidth(state,box.contentWidth()-48),box.contentX(),box.y()+29,RoyalPalette.TEXT,false);
        net.minecraft.world.entity.Entity entity=null;
        for(var candidate:minecraft.level.entitiesForRendering())if(candidate.getUUID().equals(view.npc())){entity=candidate;break;}
        if(entity instanceof LivingEntity npc&&minecraft.player.distanceToSqr(npc)<64)RoyalNpcPortrait.render(g,box.x()+box.width()-50,box.y()+6,36,36,15,mx,my,npc);
        int x=box.contentX(),y=box.bodyY(),w=box.contentWidth(),h=box.bodyHeight();
        if(view.reviewAction()>0){
            RoyalPanel.parchment(g,x,y,w,h);paragraphs(g,reviewText(),x+8,y+8,w-16,h-16);
        }else if(tab==0){
            int fw=(w-8)/3;g.drawString(font,"Foundation X",x,y,RoyalPalette.MUTED,false);g.drawString(font,"Y",x+fw+4,y,RoyalPalette.MUTED,false);g.drawString(font,"Z",x+2*(fw+4),y,RoyalPalette.MUTED,false);
            if(h>116){RoyalPanel.parchment(g,x,y+94,w,h-94);paragraphs(g,List.of(text("ONE KINGDOM BUILDING · MATERIALS INCLUDED"),text("Winner: "+view.winner()+". Your plan includes every building material; Aldric uses them at the approved site."),text(view.modern()?"Aim with the held plan, rotate and adjust height. The server validates the complete site; then review construction in Project.":"Propose a clear supported Overworld site. An administrator must review its footprint and exterior work post before work begins."),text(view.modern()&&!view.blueprint().equals(com.mohymods.kingdomsmp.building.BlueprintRegistry.HUT)?com.mohymods.kingdomsmp.building.BlueprintRegistry.occupation(view.blueprint())+": "+view.resident()+". Before construction, the holder may choose a display name with /kingdom lodge name <name>.":"One permanent building and resident; lost plans are recoverable."),text(view.activity()),text(notice())),x+8,y+102,w-16,h-110);}
        }else if(tab==1){
            RoyalProgress.render(g,x,y,w,8,view.placed()/(double)view.total());
            g.drawString(font,"Placed: "+view.placed()+" / "+view.total(),x,y+13,RoyalPalette.TEXT,false);
            int offset=view.phase().equals("APPROVED")&&view.owner()?80:56;
            if(h>offset+16){RoyalPanel.parchment(g,x,y+offset,w,h-offset);paragraphs(g,List.of(text(greeting),text(view.activity()),text(siteText()),text(view.modern()?view.blueprint().equals(com.mohymods.kingdomsmp.building.BlueprintRegistry.HUT)?"Aldric Stonehand · Builder. Your starting construction service.":view.phase().equals("COMPLETE")?view.resident()+" · "+com.mohymods.kingdomsmp.building.BlueprintRegistry.occupation(view.blueprint())+". Visit the resident or service lectern.":view.resident()+" arrives after the complete "+view.title()+" is verified.":view.phase().equals("COMPLETE")?"Mira Ashford · Reeve. Visit her or the milestone lectern to inspect the kingdom's progress.":"Mira Ashford, the Reeve, will arrive after the whole Lodge is built and verified."),text(view.modern()?"Royal services: Architect, Essentials and Mailbox. Banking requires the completed Tier 3 Royal Bank.":"Legacy Lodge entitlement preserved. Banking, Royal Trading and Charter follow their own unlocks."),text(greeting.isBlank()?notice():"")),x+8,y+offset+8,w-16,h-offset-16);}
        }else{
            RoyalPanel.parchment(g,x,y+52,w,h-52);paragraphs(g,List.of(text("ROYAL CONSTRUCTION ADMINISTRATION"),text(siteText()),text("Review the site nearby. No terrain replacement or player-property destruction is permitted."),text(view.activity()),text("Recent private construction audit:\n"+view.audit()),text(notice())),x+8,y+60,w-16,h-68);
        }
        if(ConstructionController.waiting()||ConstructionController.refreshRequired()){
            g.drawString(font,font.plainSubstrByWidth(ConstructionController.status(),w),x,box.footerY()-12,RoyalPalette.MUTED,false);
            mutations.forEach(b->b.active=false);
        }else if(tab==0&&view.reviewAction()==0&&h<=116&&!notice().isBlank()){
            g.drawString(font,font.plainSubstrByWidth(notice(),w),x,box.footerY()-12,RoyalPalette.MUTED,false);
        }
        super.render(g,mx,my,partial);
    }
    private List<Component> reviewText(){
        String heading=switch(view.reviewAction()){case 2->"CLAIM / RECOVER THE FOUNDER'S PLAN";case 4->"SUBMIT THIS EXACT SITE";case 6->"APPROVE THIS EXACT FOOTPRINT";case 8->"BEGIN ONE CONSTRUCTION PROJECT";case 10->"INSPECT AND RESUME EXISTING PROJECT";default->"REVIEW";};
        List<Component> lines=new ArrayList<>();lines.add(text(heading));lines.add(text(siteText()));
        lines.add(text(view.reviewAction()==2?"Earlier plans and site approvals become unusable. You receive one current plan, with all materials included in Aldric's construction.":view.reviewAction()==4?view.modern()?"The server will recheck the whole site and authorize valid placement. No terrain destruction is permitted.":"This submits a proposal only. An administrator must inspect and approve before construction can begin.":view.reviewAction()==6?"I have inspected this exact footprint and exterior work post. No terrain changes or destruction of player property are authorized. The winner must confirm construction separately.":view.reviewAction()==8?view.blueprint().equals(com.mohymods.kingdomsmp.building.BlueprintRegistry.HUT)?"Your plan establishes the furnished Hut immediately and issues the single registered Aldric. No second Hut can be redeemed.":"Your plan is used permanently for this kingdom-owned building. Aldric builds progressively with included materials.":"Resume the same inspected project. Missing blocks and NPCs require administrator recovery."));
        lines.add(text("Review expires after five minutes. If the site or proposal changes, review it again."));lines.add(text(view.activity()));lines.add(text(notice()));return lines;
    }
    private String siteText(){return view.hasSite()?"Overworld · "+view.origin().toShortString()+" · "+new String[]{"North","East","South","West"}[view.rotation()]+" · "+view.width()+" × "+view.depth()+" × "+view.height()+" · "+(view.valid()?"No obstruction at last review":"Site requires attention"):"No construction site selected.";}
    private String notice(){return !localNotice.isBlank()?localNotice:!ConstructionController.status().isBlank()?ConstructionController.status():view.notice();}
    private void paragraphs(GuiGraphics g,List<Component> paragraphs,int x,int y,int w,int h){
        if(h<=0)return;List<FormattedCharSequence> lines=new ArrayList<>();for(var p:paragraphs){if(p.getString().isBlank())continue;lines.addAll(font.split(p,w-6));lines.add(FormattedCharSequence.EMPTY);}
        maxScroll=Math.max(0,lines.size()*12-h);scroll=Math.clamp(scroll,0,maxScroll);g.enableScissor(x,y,x+w,y+h);
        for(int i=0;i<lines.size();i++){
            int lineY=y+i*12-scroll;
            if(lineY>=y&&lineY+font.lineHeight<=y+h)g.drawString(font,lines.get(i),x,lineY,RoyalPalette.INK,false);
        }g.disableScissor();
        if(maxScroll>0){int thumb=Math.max(6,h*h/(h+maxScroll)),top=y+(h-thumb)*scroll/maxScroll;g.fill(x+w-2,top,x+w,top+thumb,RoyalPalette.BRONZE);}
    }
    @Override public boolean mouseScrolled(double x,double y,double h,double v){scroll=Math.clamp(scroll-(int)(v*18),0,maxScroll);return true;}
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==GLFW.GLFW_KEY_PAGE_UP||key==GLFW.GLFW_KEY_PAGE_DOWN){scroll=Math.clamp(scroll+(key==GLFW.GLFW_KEY_PAGE_DOWN?36:-36),0,maxScroll);return true;}return super.keyPressed(key,scan,mods);}
    @Override public void onClose(){remember();minecraft.setScreen(parent);}
    @Override public void removed(){ConstructionController.close();}
    @Override public boolean isPauseScreen(){return false;}
    private static Component text(String text){return Component.literal(text);}
}
