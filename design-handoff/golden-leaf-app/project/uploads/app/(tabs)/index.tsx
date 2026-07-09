import { useFocusEffect, useRouter } from "expo-router";
import { useCallback, useMemo, useState } from "react";
import { Image, Pressable, ScrollView, StyleSheet, View, useWindowDimensions } from "react-native";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { ThemedText } from "@/components/themed-text";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { readCacheItems, writeCache } from "@/lib/offline-cache";
import { apiFetchJson } from "@/lib/inspection-storage";

type Module = {
  key: string;
  title: string;
  subtitle: string;
  icon: string;
  iconBg: string;
  iconColor: string;
  href: string;
};

type Notification = {
  id: string;
};

type NotifState = {
  read: string[];
  fav: string[];
  archive: string[];
  deleted: string[];
};

const modules: Module[] = [
  { key: "growers", title: "Growers", subtitle: "Registration", icon: "person-add", iconBg: "#E6F4EA", iconColor: Colors.light.primary, href: "/registration" },
  { key: "permits", title: "Permits", subtitle: "Transport", icon: "description", iconBg: "#FFEFD6", iconColor: "#D97706", href: "/permits" },
  { key: "sales", title: "Sales", subtitle: "Capture", icon: "storefront", iconBg: "#E6F0FF", iconColor: "#2563EB", href: "/marketing" },
  { key: "inspection", title: "Inspection", subtitle: "Field & Curing", icon: "eco", iconBg: "#EAF6EA", iconColor: Colors.light.primary, href: "/inspection" },
  { key: "arbitration", title: "Arbitration", subtitle: "Disputes", icon: "gavel", iconBg: "#FCE7E7", iconColor: "#DC2626", href: "/arbitration" },
  { key: "sync", title: "Sync", subtitle: "Offline Data", icon: "sync", iconBg: "#E6F0FF", iconColor: "#2563EB", href: "/sync-settings" },
];

export default function HomeScreen() {
  const router = useRouter();
  const theme = "light";
  const insets = useSafeAreaInsets();
  const { width } = useWindowDimensions();

  const [profile, setProfile] = useState<any>(null);
  const [unreadNotifCount, setUnreadNotifCount] = useState<number>(0);
  const [dashboardCounts, setDashboardCounts] = useState<{
    growers: number | "-";
    inspectionsDue: number | "-";
    permits: number | "-";
  }>({
    growers: "-",
    inspectionsDue: "-",
    permits: "-",
  });

  const countFromPayload = useCallback((payload: any): number => {
    if (payload && typeof payload.count === "number") return payload.count;
    if (Array.isArray(payload)) return payload.length;
    const results = Array.isArray(payload?.results) ? payload.results : null;
    if (results) return results.length;
    return 0;
  }, []);

  const listFromPayload = useCallback((payload: any): any[] => {
    if (Array.isArray(payload)) return payload;
    if (payload && Array.isArray(payload.results)) return payload.results;
    return [];
  }, []);

  const fetchProfileName = useCallback(async () => {
    const cached = await readCacheItems<any>("auth_me");
    if (cached[0]) setProfile(cached[0]);

    try {
      const resp = await apiFetchJson("/api/v1/auth/me/", { method: "GET" });
      if (resp.ok) {
        const data = JSON.parse(resp.body);
        setProfile(data);
        await writeCache("auth_me", [data]);
      }
    } catch {
      // Keep showing cached profile if available.
    }
  }, []);

  const fetchUnreadNotificationsCount = useCallback(async () => {
    const cached = await readCacheItems<any>("notifications_unread_count");
    const cachedCount = cached?.[0]?.count;
    if (typeof cachedCount === "number") setUnreadNotifCount(cachedCount);

    try {
      const STORAGE_KEY = "tbz_notif_state_v1";
      const rawState = await AsyncStorage.getItem(STORAGE_KEY);
      const state: NotifState =
        rawState && typeof rawState === "string"
          ? (JSON.parse(rawState) as NotifState)
          : { read: [], fav: [], archive: [], deleted: [] };

      const resp = await apiFetchJson("/api/v1/notifications/my/");
      if (!resp.ok) return;

      const payload = JSON.parse(resp.body);
      const rows = listFromPayload(payload) as Notification[];

      const read = new Set(Array.isArray(state.read) ? state.read : []);
      const archive = new Set(Array.isArray(state.archive) ? state.archive : []);
      const deleted = new Set(Array.isArray(state.deleted) ? state.deleted : []);

      const count = rows.filter((n) => n?.id && !deleted.has(n.id) && !archive.has(n.id) && !read.has(n.id)).length;
      setUnreadNotifCount(count);
      void writeCache("notifications_unread_count", [{ count, updatedAt: Date.now() }]);
    } catch {
    }
  }, [listFromPayload]);

  const fetchDashboardCounts = useCallback(async () => {
    const cached = await readCacheItems<any>("dashboard_counts");
    const cachedCounts = cached?.[0];
    if (cachedCounts && typeof cachedCounts === "object") {
      setDashboardCounts({
        growers: typeof cachedCounts.growers === "number" ? cachedCounts.growers : "-",
        inspectionsDue: typeof cachedCounts.inspectionsDue === "number" ? cachedCounts.inspectionsDue : "-",
        permits: typeof cachedCounts.permits === "number" ? cachedCounts.permits : "-",
      });
    }

    try {
      const [growersResp, permitsResp, inspectionsResp] = await Promise.all([
        apiFetchJson("/api/v1/growers/growers/?limit=1", { method: "GET" }),
        apiFetchJson("/api/v1/permits/transport-permits/?limit=1", { method: "GET" }),
        apiFetchJson("/api/v1/inspectorate/inspections/?limit=1&status=SCHEDULED", { method: "GET" }),
      ]);

      setDashboardCounts((prev) => {
        const next = { ...prev };
        if (growersResp.ok) next.growers = countFromPayload(JSON.parse(growersResp.body));
        if (permitsResp.ok) next.permits = countFromPayload(JSON.parse(permitsResp.body));
        if (inspectionsResp.ok) next.inspectionsDue = countFromPayload(JSON.parse(inspectionsResp.body));
        void writeCache("dashboard_counts", [next]);
        return next;
      });
    } catch {
    }
  }, [countFromPayload]);

  useFocusEffect(
    useCallback(() => {
      fetchProfileName();
      fetchDashboardCounts();
      fetchUnreadNotificationsCount();
    }, [fetchDashboardCounts, fetchProfileName, fetchUnreadNotificationsCount])
  );

  const displayName = useMemo(() => {
    const full = String(profile?.full_name ?? "").trim();
    if (full) return full;
    const first = String(profile?.first_name ?? "").trim();
    const last = String(profile?.last_name ?? "").trim();
    return `${first} ${last}`.trim();
  }, [profile]);

  const initials = useMemo(() => {
    const a = String(profile?.first_name?.[0] ?? "").trim().toUpperCase();
    const b = String(profile?.last_name?.[0] ?? "").trim().toUpperCase();
    return `${a}${b}` || "GL";
  }, [profile]);

  const { tileSize, tileColumns } = useMemo(() => {
    const gap = 12;
    const padding = 20 * 2;
    const min = 112;

    const availableFor3 = width - padding - gap * 2;
    const size3 = Math.floor(availableFor3 / 3);
    if (size3 >= min) return { tileSize: size3, tileColumns: 3 as const };

    const availableFor2 = width - padding - gap * 1;
    const size2 = Math.floor(availableFor2 / 2);
    return { tileSize: Math.max(min, size2), tileColumns: 2 as const };
  }, [width]);

  const actions = useMemo(() => [...modules], []);

  return (
    <View style={styles.container}>
      <View style={[styles.header, { paddingTop: insets.top + 8, backgroundColor: Colors[theme].primary }]}>
        <View style={styles.headerBg}>
          <View style={styles.headerCircleOne} />
          <View style={styles.headerCircleTwo} />
          <View style={styles.headerCircleThree} />
        </View>

        <View style={styles.headerRow}>
          <View style={styles.brandMark}>
            <Image
              source={require("../../assets/images/icon.png")}
              style={styles.brandLogo}
              resizeMode="contain"
            />
            <ThemedText style={styles.brandName} numberOfLines={1} ellipsizeMode="tail">
              Golden leaf
            </ThemedText>
          </View>

          <Pressable onPress={() => router.push("/notifications")} style={({ pressed }) => [styles.headerIconBtn, pressed && { opacity: 0.9 }]}>
            <MaterialIcons name="notifications-none" size={26} color={Colors[theme].surface} />
            {unreadNotifCount > 0 && (
              <View style={styles.badge}>
                <ThemedText style={styles.badgeText}>{unreadNotifCount > 99 ? "99+" : String(unreadNotifCount)}</ThemedText>
              </View>
            )}
          </Pressable>
        </View>

        <Pressable
          onPress={() => router.push("/profile")}
          style={({ pressed }) => [styles.dashboardCard, pressed && { opacity: 0.96 }]}
        >
          <View style={styles.dashboardBgArc} />
          <View style={styles.dashboardCircleA} />
          <View style={styles.dashboardCircleB} />

          <View style={styles.dashboardAvatarWrap}>
            <ThemedText style={styles.dashboardAvatarText}>{initials}</ThemedText>
          </View>

          <ThemedText style={styles.dashboardKicker}>GOLDEN LEAF DASHBOARD</ThemedText>
          <ThemedText style={styles.dashboardName} numberOfLines={1} ellipsizeMode="tail">
            {displayName || "Profile"}
          </ThemedText>
          <ThemedText style={styles.dashboardSub} numberOfLines={1} ellipsizeMode="tail">
            Tap to view profile
          </ThemedText>

          <View style={styles.dashboardStatsRow}>
            <View style={styles.statBlock}>
              <ThemedText style={styles.statLabel}>Growers</ThemedText>
              <ThemedText style={styles.statValue} numberOfLines={1} ellipsizeMode="tail">
                {String(dashboardCounts.growers)}
              </ThemedText>
            </View>
            <View style={styles.statDivider} />
            <View style={styles.statBlock}>
              <ThemedText style={styles.statLabel}>Insp Due</ThemedText>
              <ThemedText style={[styles.statValue, { color: Colors[theme].accent }]} numberOfLines={1} ellipsizeMode="tail">
                {String(dashboardCounts.inspectionsDue)}
              </ThemedText>
            </View>
            <View style={styles.statDivider} />
            <View style={styles.statBlock}>
              <ThemedText style={styles.statLabel}>Permits</ThemedText>
              <ThemedText style={styles.statValue} numberOfLines={1} ellipsizeMode="tail">
                {String(dashboardCounts.permits)}
              </ThemedText>
            </View>
          </View>
        </Pressable>
      </View>

      <View style={styles.sheet}>
        <ScrollView contentContainerStyle={styles.sheetContent} bounces={false}>
          <ThemedText style={styles.sectionTitle}>Quick Actions</ThemedText>

          <View style={[styles.tilesWrap, tileColumns === 2 && styles.tilesWrapTwoCol]}>
            {actions.slice(0, 9).map((m) => (
              <Pressable
                key={m.key}
                onPress={() => router.push(m.href as any)}
                style={({ pressed }) => [
                  styles.tile,
                  {
                    width: tileSize,
                    height: tileSize,
                    opacity: pressed ? 0.9 : 1,
                  },
                ]}
              >
                <View style={[styles.tileIconCircle, { backgroundColor: m.iconBg }]}>
                  <MaterialIcons name={m.icon as any} size={26} color={m.iconColor} />
                </View>

                <ThemedText style={styles.tileTitle} numberOfLines={1}>
                  {m.title}
                </ThemedText>
                <ThemedText style={styles.tileSub} numberOfLines={1}>
                  {m.subtitle}
                </ThemedText>
              </Pressable>
            ))}
          </View>

          <Pressable onPress={() => router.push("/guidelines")} style={({ pressed }) => [styles.guidelinesCta, pressed && { opacity: 0.92 }]}>
            <View style={styles.guidelinesCtaIcon}>
              <MaterialIcons name="description" size={18} color={Colors[theme].surface} />
            </View>
            <View style={styles.guidelinesCtaTextWrap}>
              <ThemedText style={styles.guidelinesCtaTitle} numberOfLines={1} ellipsizeMode="tail">
                Review TBZ Regulatory Guidelines
              </ThemedText>
              <ThemedText style={styles.guidelinesCtaSub} numberOfLines={1} ellipsizeMode="tail">
                Stay compliant · Updated 2026
              </ThemedText>
            </View>
            <MaterialIcons name="chevron-right" size={22} color={Colors[theme].surface} />
          </Pressable>
        </ScrollView>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#0B6B3A",
  },
  header: {
    paddingHorizontal: 20,
    paddingBottom: 18,
  },
  headerBg: {
    ...StyleSheet.absoluteFillObject,
    overflow: "hidden",
  },
  headerCircleOne: {
    position: "absolute",
    width: 260,
    height: 260,
    borderRadius: 130,
    right: -120,
    top: 40,
    backgroundColor: "rgba(0,0,0,0.15)",
  },
  headerCircleTwo: {
    position: "absolute",
    width: 190,
    height: 190,
    borderRadius: 95,
    right: -70,
    top: 0,
    backgroundColor: "rgba(255,255,255,0.08)",
  },
  headerCircleThree: {
    position: "absolute",
    width: 220,
    height: 220,
    borderRadius: 110,
    right: -150,
    top: -60,
    backgroundColor: "rgba(0,0,0,0.18)",
  },
  headerRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    justifyContent: "space-between",
  },
  brandMark: { flexDirection: "row", alignItems: "center", gap: 8, maxWidth: 170, flexShrink: 1 },
  brandLogo: { width: 25, height: 25 },
  brandName: {
    fontSize: 25,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    color: "#D4AF37",
  },
  headerIconBtn: { width: 44, height: 44, borderRadius: 22, alignItems: "center", justifyContent: "center" },
  badge: {
    position: "absolute",
    right: 10,
    top: 10,
    backgroundColor: "#EF4444",
    minWidth: 18,
    height: 18,
    paddingHorizontal: 5,
    borderRadius: 9,
    alignItems: "center",
    justifyContent: "center",
  },
  badgeText: {
    fontSize: 11,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    color: "#FFFFFF",
  },
  sheet: {
    flex: 1,
    backgroundColor: "#FFFFFF",
    borderTopLeftRadius: 34,
    borderTopRightRadius: 34,
  },
  sheetContent: {
    paddingHorizontal: 20,
    paddingTop: 18,
    paddingBottom: 28,
  },
  dashboardCard: {
    width: "100%",
    height: 186,
    borderRadius: 22,
    paddingHorizontal: 18,
    paddingVertical: 16,
    overflow: "hidden",
    backgroundColor: "#0A3B22",
    shadowColor: "#000000",
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.18,
    shadowRadius: 18,
    elevation: 5,
    marginTop: 14,
  },
  dashboardAvatarWrap: {
    position: "absolute",
    right: 16,
    top: 14,
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: "rgba(255,255,255,0.12)",
    borderWidth: 1,
    borderColor: "rgba(255,255,255,0.18)",
    alignItems: "center",
    justifyContent: "center",
  },
  dashboardAvatarText: {
    fontSize: 14,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    color: "#FFFFFF",
  },
  dashboardBgArc: {
    position: "absolute",
    width: 260,
    height: 260,
    borderRadius: 130,
    left: -140,
    top: -80,
    backgroundColor: "rgba(0,0,0,0.25)",
  },
  dashboardCircleA: {
    position: "absolute",
    width: 210,
    height: 210,
    borderRadius: 105,
    right: -85,
    top: -60,
    backgroundColor: "rgba(0,0,0,0.18)",
  },
  dashboardCircleB: {
    position: "absolute",
    width: 160,
    height: 160,
    borderRadius: 80,
    right: -40,
    top: 8,
    backgroundColor: "rgba(255,255,255,0.10)",
  },
  dashboardKicker: {
    fontSize: 11,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.medium,
    letterSpacing: 2,
    color: "rgba(255,255,255,0.60)",
  },
  dashboardName: {
    marginTop: 4,
    fontSize: 20,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    color: "#FFFFFF",
  },
  dashboardSub: {
    marginTop: 2,
    fontSize: 12,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
    color: "rgba(255,255,255,0.70)",
  },
  dashboardStatsRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 14,
  },
  statBlock: { flex: 1 },
  statLabel: {
    fontSize: 11,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
    color: "rgba(255,255,255,0.65)",
  },
  statValue: {
    marginTop: 2,
    fontSize: 20,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    color: "#FFFFFF",
  },
  statDivider: {
    width: 1,
    height: 34,
    backgroundColor: "rgba(255,255,255,0.18)",
    marginHorizontal: 14,
  },
  sectionTitle: {
    fontSize: 15,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    color: "#111827",
    marginBottom: 12,
  },
  tilesWrap: {
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "space-between",
    rowGap: 12,
  },
  tilesWrapTwoCol: {
    justifyContent: "flex-start",
    columnGap: 12,
  },
  tile: {
    borderWidth: 0,
    borderRadius: 18,
    alignItems: "center",
    justifyContent: "flex-start",
    paddingHorizontal: 10,
    paddingTop: 14,
    paddingBottom: 12,
    backgroundColor: "#F3F4F6",
    overflow: "hidden",
    shadowColor: "#000000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.06,
    shadowRadius: 10,
    elevation: 1,
  },
  tileIconCircle: {
    width: 52,
    height: 52,
    borderRadius: 26,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 10,
  },
  tileTitle: {
    fontSize: 12,
    textAlign: "center",
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.medium,
    color: "#111827",
    maxWidth: "100%",
    lineHeight: 14,
  },
  tileSub: {
    marginTop: 2,
    fontSize: 10,
    textAlign: "center",
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
    color: "#6B7280",
    maxWidth: "100%",
    lineHeight: 12,
  },
  guidelinesCta: {
    marginTop: 16,
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 18,
    paddingHorizontal: 14,
    paddingVertical: 12,
    backgroundColor: Colors.light.primary,
  },
  guidelinesCtaIcon: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: "rgba(255,255,255,0.18)",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },
  guidelinesCtaTextWrap: { flex: 1 },
  guidelinesCtaTitle: {
    fontSize: 12,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    color: Colors.light.surface,
  },
  guidelinesCtaSub: {
    marginTop: 2,
    fontSize: 10,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
    color: "rgba(255,255,255,0.78)",
  },
});
