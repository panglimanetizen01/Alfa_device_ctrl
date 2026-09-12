package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

/** Live, read-only network evidence. No mock connectivity/routing values are emitted. */
public final class AlfaNetworkPanel {
    private AlfaNetworkPanel() { }

    public static View build(Activity activity) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), 0);
        root.setBackgroundColor(AlfaUiTheme.CANVAS);
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(activity, "ALFA RF  •  NETWORK EVIDENCE", AlfaUiTheme.CYAN, 14, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(activity, 52), 1));
        Button refresh = button(activity, "REFRESH", AlfaUiTheme.CYAN);
        header.addView(refresh, new LinearLayout.LayoutParams(dp(activity, 90), dp(activity, 48)));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(activity, 56)));
        ScrollView scroll = new ScrollView(activity);
        TextView evidence = text(activity, collect(activity), AlfaUiTheme.TEXT, 11, true);
        evidence.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 20));
        scroll.addView(evidence);
        refresh.setOnClickListener(v -> evidence.setText(collect(activity)));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private static String collect(Activity activity) {
        StringBuilder out = new StringBuilder();
        out.append("CONNECTIVITY\n");
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return out.append("status=UNAVAILABLE\n").toString();
        Network active = cm.getActiveNetwork();
        NetworkCapabilities caps = active == null ? null : cm.getNetworkCapabilities(active);
        LinkProperties links = active == null ? null : cm.getLinkProperties(active);
        out.append("active_network=").append(active == null ? "NONE" : active).append('\n');
        out.append("internet=").append(has(caps, NetworkCapabilities.NET_CAPABILITY_INTERNET)).append('\n');
        out.append("validated=").append(has(caps, NetworkCapabilities.NET_CAPABILITY_VALIDATED)).append('\n');
        out.append("vpn=").append(hasTransport(caps, NetworkCapabilities.TRANSPORT_VPN)).append('\n');
        out.append("wifi=").append(hasTransport(caps, NetworkCapabilities.TRANSPORT_WIFI)).append('\n');
        out.append("cellular=").append(hasTransport(caps, NetworkCapabilities.TRANSPORT_CELLULAR)).append('\n');
        out.append("ethernet=").append(hasTransport(caps, NetworkCapabilities.TRANSPORT_ETHERNET)).append('\n');
        if (links != null) {
            out.append("interface=").append(links.getInterfaceName()).append('\n');
            out.append("routes=").append(links.getRoutes()).append('\n');
            out.append("dns=").append(links.getDnsServers()).append('\n');
            out.append("domains=").append(links.getDomains()).append('\n');
            out.append("link_addresses=").append(links.getLinkAddresses()).append('\n');
            out.append("proxy=").append(links.getHttpProxy()).append('\n');
        }
        out.append("\nINTERFACES\n");
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                out.append(nic.getName()).append(" up=").append(nic.isUp()).append(" mtu=").append(nic.getMTU()).append('\n');
                for (InetAddress address : Collections.list(nic.getInetAddresses())) out.append("  addr=").append(address.getHostAddress()).append('\n');
            }
        } catch (Exception e) {
            out.append("interface_enumeration=ERROR ").append(e.getClass().getSimpleName()).append('\n');
        }
        out.append("\nKERNEL ROUTE SNAPSHOT\n");
        appendFile(out, "/proc/net/route");
        out.append("\nIPv6 ROUTE SNAPSHOT\n");
        appendFile(out, "/proc/net/ipv6_route");
        out.append("\nBOUNDARY\n");
        out.append("DNS/routing above are observations from Android/Linux APIs/files. Firewall/NAT rule inspection is not claimed here because it requires privileges or platform-specific access not established by this UI layer.\n");
        return out.toString();
    }

    private static boolean has(NetworkCapabilities c, int capability) { return c != null && c.hasCapability(capability); }
    private static boolean hasTransport(NetworkCapabilities c, int transport) { return c != null && c.hasTransport(transport); }
    private static void appendFile(StringBuilder out, String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line; int count = 0;
            while ((line = reader.readLine()) != null && count++ < 128) out.append(line).append('\n');
        } catch (Exception e) { out.append(path).append("=UNAVAILABLE (").append(e.getClass().getSimpleName()).append(")\n"); }
    }
    private static TextView text(Activity a, String s, int c, int sp, boolean mono) { TextView t = new TextView(a); t.setText(s); t.setTextColor(c); t.setTextSize(sp); if (mono) t.setTypeface(Typeface.MONOSPACE); return t; }
    private static Button button(Activity a, String s, int c) { Button b = new Button(a); b.setText(s); b.setTextColor(c); b.setTextSize(10); b.setMinHeight(dp(a, 48)); return b; }
    private static int dp(Activity a, int value) { return Math.round(value * a.getResources().getDisplayMetrics().density); }
}
