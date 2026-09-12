package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.RouteInfo;
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
import java.util.Locale;

/** Live, bounded, read-only Android/Linux network evidence. No mock connectivity is emitted. */
public final class AlfaNetworkPanel {
    private static final int MAX_PROC_LINES = 128;
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
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        out.append("NETWORK FORENSIC SNAPSHOT\n");
        out.append("==========================\n");
        if (cm == null) return out.append("connectivity_manager=UNAVAILABLE\n").toString();

        Network active = cm.getActiveNetwork();
        NetworkCapabilities caps = active == null ? null : cm.getNetworkCapabilities(active);
        LinkProperties links = active == null ? null : cm.getLinkProperties(active);

        out.append("active_network=").append(active == null ? "NONE" : active).append('\n');
        out.append("internet_configured=").append(has(caps, NetworkCapabilities.NET_CAPABILITY_INTERNET)).append('\n');
        out.append("internet_validated=").append(has(caps, NetworkCapabilities.NET_CAPABILITY_VALIDATED)).append('\n');
        out.append("not_metered=").append(has(caps, NetworkCapabilities.NET_CAPABILITY_NOT_METERED)).append('\n');
        out.append("captive_portal=").append(has(caps, NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)).append('\n');
        out.append("trusted=").append(has(caps, NetworkCapabilities.NET_CAPABILITY_TRUSTED)).append('\n');
        out.append("vpn_transport=").append(hasTransport(caps, NetworkCapabilities.TRANSPORT_VPN)).append('\n');
        out.append("wifi_transport=").append(hasTransport(caps, NetworkCapabilities.TRANSPORT_WIFI)).append('\n');
        out.append("cellular_transport=").append(hasTransport(caps, NetworkCapabilities.TRANSPORT_CELLULAR)).append('\n');
        out.append("ethernet_transport=").append(hasTransport(caps, NetworkCapabilities.TRANSPORT_ETHERNET)).append('\n');
        out.append("downstream_kbps=").append(caps == null ? "UNKNOWN" : caps.getLinkDownstreamBandwidthKbps()).append('\n');
        out.append("upstream_kbps=").append(caps == null ? "UNKNOWN" : caps.getLinkUpstreamBandwidthKbps()).append('\n');

        if (links != null) {
            out.append("\nLINK PROPERTIES\n");
            out.append("interface=").append(valueOrUnknown(links.getInterfaceName())).append('\n');
            out.append("mtu=").append(links.getMtu()).append('\n');
            out.append("domains=").append(valueOrUnknown(links.getDomains())).append('\n');
            out.append("dns=").append(links.getDnsServers()).append('\n');
            out.append("proxy=").append(valueOrUnknown(links.getHttpProxy())).append('\n');
            out.append("link_addresses=").append(links.getLinkAddresses()).append('\n');
            out.append("routes=").append(links.getRoutes()).append('\n');
            appendRouteBreakdown(out, links);
        }

        out.append("\nINTERFACES\n");
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                out.append(nic.getName())
                        .append(" up=").append(nic.isUp())
                        .append(" loopback=").append(nic.isLoopback())
                        .append(" mtu=").append(nic.getMTU()).append('\n');
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    out.append("  addr=").append(address.getHostAddress()).append('\n');
                }
            }
        } catch (Exception e) {
            out.append("interface_enumeration=ERROR ").append(e.getClass().getSimpleName()).append('\n');
        }

        out.append("\nKERNEL ROUTES\n");
        appendFile(out, "/proc/net/route");
        out.append("\nKERNEL IPv6 ROUTES\n");
        appendFile(out, "/proc/net/ipv6_route");

        out.append("\nSOCKET TABLE SNAPSHOT\n");
        appendSocketSummary(out, "/proc/net/tcp", "TCP4");
        appendSocketSummary(out, "/proc/net/tcp6", "TCP6");
        appendSocketSummary(out, "/proc/net/udp", "UDP4");
        appendSocketSummary(out, "/proc/net/udp6", "UDP6");

        out.append("\nBOUNDARY\n");
        out.append("Observed: Android ConnectivityManager/NetworkCapabilities/LinkProperties, Java NetworkInterface, bounded /proc route/socket snapshots.\n");
        out.append("Not claimed: firewall policy, NAT table, packet capture, VPN encryption, proxy enforcement, or network-namespace isolation. Those require direct privileged/platform evidence.\n");
        return out.toString();
    }

    private static void appendRouteBreakdown(StringBuilder out, LinkProperties links) {
        int ipv4 = 0, ipv6 = 0, defaultRoutes = 0;
        for (RouteInfo route : links.getRoutes()) {
            if (route.getDestination() == null) continue;
            String address = route.getDestination().getAddress().getHostAddress();
            if (address.contains(":")) ipv6++; else ipv4++;
            if (route.isDefaultRoute()) defaultRoutes++;
        }
        out.append("route_count_ipv4=").append(ipv4).append('\n');
        out.append("route_count_ipv6=").append(ipv6).append('\n');
        out.append("default_route_count=").append(defaultRoutes).append('\n');
    }

    private static void appendSocketSummary(StringBuilder out, String path, String label) {
        int count = 0;
        String firstPorts = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line; int lines = 0;
            while ((line = reader.readLine()) != null && lines++ < MAX_PROC_LINES + 1) {
                if (line.startsWith("  ")) continue;
                if (line.startsWith("sl ")) continue;
                String[] fields = line.trim().split("\\s+");
                if (fields.length < 4) continue;
                count++;
                if (count <= 8) {
                    String local = fields[1];
                    int colon = local.lastIndexOf(':');
                    if (colon >= 0) {
                        String hexPort = local.substring(colon + 1);
                        try {
                            int port = Integer.parseInt(hexPort, 16);
                            if (!firstPorts.isEmpty()) firstPorts += ",";
                            firstPorts += Integer.toString(port);
                        } catch (NumberFormatException ignored) { }
                    }
                }
            }
            out.append(label).append(" entries=").append(count).append(" sample_local_ports=").append(firstPorts.isEmpty() ? "NONE" : firstPorts).append('\n');
        } catch (Exception e) {
            out.append(label).append("=UNAVAILABLE (").append(e.getClass().getSimpleName()).append(")\n");
        }
    }

    private static boolean has(NetworkCapabilities c, int capability) { return c != null && c.hasCapability(capability); }
    private static boolean hasTransport(NetworkCapabilities c, int transport) { return c != null && c.hasTransport(transport); }
    private static String valueOrUnknown(Object value) { return value == null ? "UNKNOWN" : String.format(Locale.US, "%s", value); }

    private static void appendFile(StringBuilder out, String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line; int count = 0;
            while ((line = reader.readLine()) != null && count++ < MAX_PROC_LINES) out.append(line).append('\n');
        } catch (Exception e) {
            out.append(path).append("=UNAVAILABLE (").append(e.getClass().getSimpleName()).append(")\n");
        }
    }

    private static TextView text(Activity a, String s, int c, int sp, boolean mono) {
        TextView t = new TextView(a); t.setText(s); t.setTextColor(c); t.setTextSize(sp);
        if (mono) t.setTypeface(Typeface.MONOSPACE); return t;
    }

    private static Button button(Activity a, String s, int c) {
        Button b = new Button(a); b.setText(s); b.setTextColor(c); b.setTextSize(10); b.setMinHeight(dp(a, 48)); return b;
    }

    private static int dp(Activity a, int value) { return Math.round(value * a.getResources().getDisplayMetrics().density); }
}
