package com.alfa.device_ctrl;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.termux.view.TerminalView;

/** Reattaches service-owned runtime sessions to a newly recreated canonical Stitch activity. */
final class RuntimeSessionReattachment {
    private static final String TAG_PREFIX = "alfa-runtime-session:";

    private RuntimeSessionReattachment() { }

    static boolean attach(Activity activity, RuntimeSessionManager manager) {
        if (!(activity instanceof StitchOperationalActivity) || manager == null || !manager.isRunning()) return false;
        try {
            String sessionId=manager.currentSession().mSessionName;
            java.lang.reflect.Field multiplexerField=StitchOperationalActivity.class.getDeclaredField("sessionMultiplexer");
            multiplexerField.setAccessible(true);
            RuntimeSessionMultiplexer multiplexer=(RuntimeSessionMultiplexer)multiplexerField.get(activity);
            java.lang.reflect.Method listenerMethod=StitchOperationalActivity.class.getDeclaredMethod("sessionListener",String.class);
            listenerMethod.setAccessible(true);
            RuntimeSessionManager.Listener listener=(RuntimeSessionManager.Listener)listenerMethod.invoke(activity,sessionId);
            multiplexer.adoptSession(sessionId,manager,listener);
            TerminalView terminal=findTerminalView(activity.findViewById(android.R.id.content),sessionId);
            if(terminal==null)return false;
            manager.attachTo(terminal);
            return terminal.mTermSession==manager.currentSession();
        } catch (ReflectiveOperationException|RuntimeException ignored) { return false; }
    }

    private static TerminalView findTerminalView(View view,String sessionId){
        String tag=TAG_PREFIX+sessionId;
        TerminalView tagged=findTagged(view,tag);
        if(tagged!=null)return tagged;
        TerminalView untagged=findUntagged(view);
        if(untagged!=null){untagged.setTag(tag);return untagged;}
        if(!(view instanceof ViewGroup))return null;
        ViewGroup root=(ViewGroup)view;
        TerminalView created=new TerminalView(root.getContext(),null);
        created.setTag(tag);
        created.setVisibility(View.INVISIBLE);
        root.addView(created,new FrameLayout.LayoutParams(1,1));
        return created;
    }

    private static TerminalView findTagged(View view,String tag){
        if(view instanceof TerminalView&&tag.equals(view.getTag()))return(TerminalView)view;
        if(!(view instanceof ViewGroup))return null;
        ViewGroup group=(ViewGroup)view;
        for(int i=0;i<group.getChildCount();i++){TerminalView found=findTagged(group.getChildAt(i),tag);if(found!=null)return found;}
        return null;
    }

    private static TerminalView findUntagged(View view){
        if(view instanceof TerminalView&&view.getTag()==null)return(TerminalView)view;
        if(!(view instanceof ViewGroup))return null;
        ViewGroup group=(ViewGroup)view;
        for(int i=0;i<group.getChildCount();i++){TerminalView found=findUntagged(group.getChildAt(i));if(found!=null)return found;}
        return null;
    }

    static TerminalView findTerminalView(View view){if(view instanceof TerminalView)return(TerminalView)view;if(!(view instanceof ViewGroup))return null;ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){TerminalView found=findTerminalView(group.getChildAt(i));if(found!=null)return found;}return null;}
}
