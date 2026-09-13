package com.alfa.device_ctrl;

/**
 * Native state contract for the supplied Stitch v1 catalog.
 *
 * The catalog is treated as an implementation input: each state resolves to a native
 * Alfa surface and, where the state describes an executable operation, to an explicit
 * action. Result/simulation states are never promoted to fabricated live evidence.
 */
public final class StitchV1StateModel {
    public enum Domain {
        TERMINAL, RUNTIME, NETWORK, SECURITY, STORAGE_POLICY, PROJECT_STORAGE,
        SESSIONS, SPLIT, FLOATING, SETTINGS, OTHER
    }

    public enum Action {
        OPEN_NATIVE_SURFACE,
        RUN_RUNTIME_COMMAND,
        CONFIRM_STOP_SESSION
    }

    public static final class State {
        private final String id;
        private final Domain domain;
        private final AlfaUiNavigation.Screen primaryScreen;
        private final boolean referenceOnly;
        private final boolean claimsLiveExecution;
        private final Action action;
        private final String runtimeCommand;

        private State(String id, Domain domain, AlfaUiNavigation.Screen primaryScreen,
                      boolean referenceOnly, boolean claimsLiveExecution,
                      Action action, String runtimeCommand) {
            this.id = id;
            this.domain = domain;
            this.primaryScreen = primaryScreen;
            this.referenceOnly = referenceOnly;
            this.claimsLiveExecution = claimsLiveExecution;
            this.action = action;
            this.runtimeCommand = runtimeCommand;
        }

        public String id() { return id; }
        public String domain() { return domain.name().toLowerCase().replace('_', '-'); }
        public AlfaUiNavigation.Screen primaryScreen() { return primaryScreen; }
        public boolean referenceOnly() { return referenceOnly; }
        public boolean claimsLiveExecution() { return claimsLiveExecution; }
        public Action action() { return action; }
        public String runtimeCommand() { return runtimeCommand; }
        public int supportedActionCount() { return action == null ? 0 : 1; }
    }

    private StitchV1StateModel() { }

    public static State resolve(String id) {
        if (id == null || !StitchV1ReferenceCatalog.contains(id)) return null;
        String s = id.toLowerCase();
        Domain domain = domainOf(s);
        boolean referenceOnly = isReferenceOnly(s);
        String command = runtimeCommandOf(s);
        Action action = command == null ? Action.OPEN_NATIVE_SURFACE : Action.RUN_RUNTIME_COMMAND;
        if (s.contains("stop_session") && !referenceOnly) action = Action.CONFIRM_STOP_SESSION;
        boolean claimsLiveExecution = !referenceOnly &&
                (domain == Domain.TERMINAL || domain == Domain.RUNTIME || domain == Domain.NETWORK ||
                 domain == Domain.SESSIONS || domain == Domain.SPLIT || domain == Domain.FLOATING);
        return new State(id, domain, screenOf(domain), referenceOnly, claimsLiveExecution, action, command);
    }

    private static boolean isReferenceOnly(String s) {
        return s.contains("simulation") ||
                s.contains("maximum_sessions_reached") ||
                s.contains("primary_app_icon") ||
                s.contains("app_icon_suite_presentation") ||
                s.contains("successfully_restored_applied") ||
                s.contains("webhook_event_dispatched_success") ||
                s.contains("session_5_terminated_via_sigterm") ||
                s.contains("apt_upgrade_y_completed");
    }

    private static String runtimeCommandOf(String s) {
        if (s.contains("apt_update_executed")) return "apt update";
        if (s.contains("screen_cleared_via_clear_command")) return "clear";
        if (s.contains("uname_a_ls_la_executed")) return "uname -a && ls -la";
        return null;
    }

    private static Domain domainOf(String s) {
        if (s.contains("operational_terminal_runtime_dashboard")) return Domain.TERMINAL;
        if (s.contains("security") || s.contains("seccomp") || s.contains("shizuku") || s.contains("su_escalation") || s.contains("ptrace") || s.contains("harden_bind")) return Domain.SECURITY;
        if (s.contains("network") || s.contains("socket") || s.contains("tunnel") || s.contains("matrix") || s.contains("dns")) return Domain.NETWORK;
        if (s.contains("mount") || s.contains("override") || s.contains("sensitive_files") || s.contains("policy") || s.contains("filesystem_bind")) return Domain.STORAGE_POLICY;
        if (s.contains("project") || s.contains("file_browser") || s.contains("create_new_file") || s.contains("create_new_folder") || s.contains("directory") || s.contains("audit_vault")) return Domain.PROJECT_STORAGE;
        if (s.contains("appearance") || s.contains("font") || s.contains("language") || s.contains("settings") || s.contains("control_center")) return Domain.SETTINGS;
        if (s.contains("split")) return Domain.SPLIT;
        if (s.contains("floating")) return Domain.FLOATING;
        if (s.contains("session") || s.contains("multiplexer")) return Domain.SESSIONS;
        if (s.contains("runtime") || s.contains("linux_3.24") || s.contains("ubuntu_24.04") || s.contains("kali_nethunter")) return Domain.RUNTIME;
        if (s.contains("terminal") || s.contains("operational")) return Domain.TERMINAL;
        return Domain.OTHER;
    }

    private static AlfaUiNavigation.Screen screenOf(Domain domain) {
        switch (domain) {
            case RUNTIME: return AlfaUiNavigation.Screen.RUNTIME;
            case NETWORK: return AlfaUiNavigation.Screen.NETWORK;
            case SECURITY: return AlfaUiNavigation.Screen.SECURITY;
            case STORAGE_POLICY: return AlfaUiNavigation.Screen.STORAGE;
            case PROJECT_STORAGE: return AlfaUiNavigation.Screen.PROJECT;
            case SESSIONS: return AlfaUiNavigation.Screen.SESSIONS;
            case SPLIT: return AlfaUiNavigation.Screen.SPLIT;
            case FLOATING: return AlfaUiNavigation.Screen.FLOATING;
            case SETTINGS: return AlfaUiNavigation.Screen.SETTINGS;
            case TERMINAL: return AlfaUiNavigation.Screen.TERMINAL;
            default: return AlfaUiNavigation.Screen.DIAGNOSTICS;
        }
    }
}
