import { useCallback, useState } from "react";
import { Alert, Pressable, ScrollView, StyleSheet, View } from "react-native";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { useFocusEffect, useRouter } from "expo-router";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { LeafLoader } from "@/components/LeafLoader";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiErrorUi, apiFetchJson } from "@/lib/inspection-storage";
import { readCacheItems, writeCache } from "@/lib/offline-cache";

type TransportPermitApi = {
  id: string;
  permit_number: string | null;
  grower_name: string;
  total_bales: number;
  total_weight_kg: string;
  origin_district: string;
  destination_salesfloor: string;
  status: string;
  valid_to: string | null;
  date_requested: string;
};

function listFromApi(payload: any): { items: TransportPermitApi[]; next: string } {
  if (Array.isArray(payload)) return { items: payload as TransportPermitApi[], next: "" };
  const items = Array.isArray(payload?.results) ? (payload.results as TransportPermitApi[]) : [];
  const next = typeof payload?.next === "string" ? payload.next : "";
  return { items, next };
}

export default function PermitsScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  const [permits, setPermits] = useState<TransportPermitApi[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchAllPermits = useCallback(async () => {
    const collected: TransportPermitApi[] = [];
    let nextPath: string | null = "/api/v1/permits/transport-permits/?ordering=-date_requested&page_size=250";

    for (let page = 1; page <= 40 && nextPath; page += 1) {
      const resp = await apiFetchJson(nextPath, { method: "GET" });
      if (!resp.ok) return { ok: false as const, status: resp.status, body: resp.body, items: [] as TransportPermitApi[] };

      let payload: any = null;
      try {
        payload = JSON.parse(resp.body);
      } catch {
        return { ok: false as const, status: 500, body: "Invalid JSON response", items: [] as TransportPermitApi[] };
      }

      const { items, next } = listFromApi(payload);
      collected.push(...items);
      if (!next || items.length === 0) break;

      try {
        const u = new URL(next);
        nextPath = `${u.pathname}${u.search}`;
      } catch {
        nextPath = next.startsWith("/") ? next : `/${next}`;
      }
    }

    return { ok: true as const, status: 200, body: "", items: collected };
  }, []);

  const fetchPermits = useCallback(async () => {
    setLoading(true);
    try {
      const cached = await readCacheItems<TransportPermitApi>("permits");
      if (cached.length > 0 && permits.length === 0) setPermits(cached);

      const resp = await fetchAllPermits();
      if (resp.ok) {
        setPermits(resp.items);
        if (resp.items.length > 0) void writeCache("permits", resp.items);
      } else {
        const ui = apiErrorUi(resp.status, resp.body);
        if (ui.kind === "login_required") {
          Alert.alert(ui.title, ui.message, [
            { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: "/permits" } } as any) },
            { text: "OK" },
          ]);
        } else {
          Alert.alert(ui.title, ui.message);
        }
      }
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Network request failed.";
      Alert.alert("Network error", msg);
    } finally {
      setLoading(false);
    }
  }, [fetchAllPermits, permits.length, router]);

  useFocusEffect(
    useCallback(() => {
      fetchPermits();
    }, [fetchPermits])
  );

  const isActive = (p: TransportPermitApi) => {
    if (p.status !== "APPROVED") return false;
    if (!p.valid_to) return true;
    const toDate = new Date(p.valid_to);
    const today = new Date();
    toDate.setHours(0, 0, 0, 0);
    today.setHours(0, 0, 0, 0);
    return toDate >= today;
  };

  const isExpired = (p: TransportPermitApi) => {
    if (p.status === "EXPIRED") return true;
    if (p.status === "APPROVED" && p.valid_to) {
      const toDate = new Date(p.valid_to);
      const today = new Date();
      toDate.setHours(0, 0, 0, 0);
      today.setHours(0, 0, 0, 0);
      return toDate < today;
    }
    return false;
  };

  const total = permits.length;
  const activeCount = permits.filter(isActive).length;
  const expiredCount = permits.filter(isExpired).length;
  const pendingCount = permits.filter(p => p.status === "PENDING").length;

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.topRow}>
          <View style={styles.actionRow}>
          <Pressable
            style={({ pressed }) => [styles.secondaryPill, pressed && { opacity: 0.9 }]}
            onPress={() => router.push("/permit-validate")}
          >
            <MaterialIcons name="qr-code-scanner" size={18} color={Colors[theme].accent} />
            <ThemedText style={styles.secondaryPillText}>Validate</ThemedText>
          </Pressable>

          <Pressable
            style={({ pressed }) => [styles.primaryPill, pressed && { opacity: 0.9 }]}
            onPress={() => router.push("/permit-request")}
          >
            <MaterialIcons name="add" size={18} color="#FFFFFF" />
            <ThemedText style={styles.primaryPillText}>Request</ThemedText>
          </Pressable>
          </View>
        </View>

        <View style={styles.statsGrid}>
          <Pressable
            onPress={() => router.push({ pathname: "/permit-list", params: { filter: "ACTIVE" } })}
            style={[styles.statCard, { backgroundColor: "#E8F3EE", borderColor: Colors[theme].border }]}
          >
            <View style={styles.statTop}>
              <ThemedText style={styles.statLabel} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                Active Permits
              </ThemedText>
              <View style={[styles.statIconWrap, { backgroundColor: "#FFF7E6" }]}>
                <MaterialIcons name="local-shipping" size={18} color={Colors[theme].accent} />
              </View>
            </View>
            <ThemedText type="title" style={{ color: "#0B6B3A" }}>
              {activeCount}
            </ThemedText>
          </Pressable>

          <Pressable
            onPress={() => router.push({ pathname: "/permit-list", params: { filter: "PENDING" } })}
            style={[styles.statCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}
          >
            <View style={styles.statTop}>
              <ThemedText style={styles.statLabel} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                Pending
              </ThemedText>
              <View style={[styles.statIconWrap, { backgroundColor: "#FFF7E6" }]}>
                <MaterialIcons name="schedule" size={18} color={Colors[theme].accent} />
              </View>
            </View>
            <ThemedText type="title" style={{ color: "#F59E0B" }}>
              {pendingCount}
            </ThemedText>
          </Pressable>

          <Pressable
            onPress={() => router.push({ pathname: "/permit-list", params: { filter: "EXPIRED" } })}
            style={[styles.statCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}
          >
            <View style={styles.statTop}>
              <ThemedText style={styles.statLabel} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                Expired
              </ThemedText>
              <View style={[styles.statIconWrap, { backgroundColor: "#FFF7E6" }]}>
                <MaterialIcons name="history" size={18} color={Colors[theme].accent} />
              </View>
            </View>
            <ThemedText type="title" style={{ color: "#DC2626" }}>
              {expiredCount}
            </ThemedText>
          </Pressable>

          <Pressable
            onPress={() => router.push({ pathname: "/permit-list", params: { filter: "ALL" } })}
            style={[styles.statCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}
          >
            <View style={styles.statTop}>
              <ThemedText style={styles.statLabel} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                Total Requested
              </ThemedText>
              <View style={[styles.statIconWrap, { backgroundColor: "#FFF7E6" }]}>
                <MaterialIcons name="list-alt" size={18} color={Colors[theme].accent} />
              </View>
            </View>
            <ThemedText type="title" style={{ color: "#6B7280" }}>
              {total}
            </ThemedText>
          </Pressable>
        </View>

        <ThemedText style={{ marginTop: 10, marginBottom: 16 }} type="subtitle">
          Recent Permits
        </ThemedText>

        {loading ? (
          <View style={{ marginTop: 30, alignItems: "center" }}>
            <LeafLoader size={42} />
          </View>
        ) : permits.length === 0 ? (
          <View style={{ alignItems: "center", paddingVertical: 40, opacity: 0.6 }}>
            <MaterialIcons name="folder-open" size={48} color={Colors[theme].muted} />
            <ThemedText style={{ marginTop: 12 }}>No transport permits found.</ThemedText>
          </View>
        ) : (
          permits.slice(0, 10).map(permit => (
            <Pressable
              key={permit.id}
              onPress={() => router.push({ pathname: "/permit-detail", params: { id: permit.id } })}
              style={[styles.permitCard, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}
            >
              <View style={styles.permitTopRow}>
                <View style={[styles.permitIconBox, { backgroundColor: "#FFF7E6" }]}>
                  <MaterialIcons name="local-shipping" size={18} color={Colors[theme].accent} />
                </View>
                <View style={styles.permitTextBlock}>
                  <View style={styles.cardHeader}>
                    <ThemedText type="defaultSemiBold" style={{ flex: 1 }} numberOfLines={1}>
                      {permit.permit_number || "PENDING ISSUANCE"}
                    </ThemedText>
                    <View style={[styles.statusBadge, {
                      backgroundColor: permit.status === "APPROVED" ? "#E8F3EE" : permit.status === "PENDING" ? "#FEF3C7" : "#FEE2E2"
                    }]}>
                      <ThemedText style={{
                        fontSize: 10, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold,
                        color: permit.status === "APPROVED" ? "#0B6B3A" : permit.status === "PENDING" ? "#D97706" : "#B91C1C"
                      }} numberOfLines={1} ellipsizeMode="tail">
                        {permit.status}
                      </ThemedText>
                    </View>
                  </View>
                  <ThemedText style={styles.permitSub} numberOfLines={1}>
                    {permit.grower_name} • {permit.origin_district} → {permit.destination_salesfloor}
                  </ThemedText>
                  <ThemedText style={styles.permitSub} numberOfLines={1}>
                    Bales: {permit.total_bales} • Weight: {permit.total_weight_kg} kg
                  </ThemedText>
                </View>
                <MaterialIcons name="chevron-right" size={22} color={Colors[theme].muted} />
              </View>
            </Pressable>
          ))
        )}
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#FFFFFF" },
  content: { padding: 20, paddingBottom: 40 },
  topRow: { flexDirection: "row", alignItems: "center", justifyContent: "flex-end", gap: 12, marginBottom: 10 },
  actionRow: { flexDirection: "row", alignItems: "center", gap: 10 },
  primaryPill: { backgroundColor: "#0B6B3A", paddingHorizontal: 14, paddingVertical: 10, borderRadius: 999, flexDirection: "row", alignItems: "center", gap: 8 },
  primaryPillText: { color: "#FFFFFF", fontSize: 13, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  secondaryPill: { backgroundColor: "#FFFFFF", borderWidth: 1, borderColor: "#E5E7EB", paddingHorizontal: 14, paddingVertical: 10, borderRadius: 999, flexDirection: "row", alignItems: "center", gap: 8 },
  secondaryPillText: { color: "#152036", fontSize: 13, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  statsGrid: { flexDirection: "row", flexWrap: "wrap", gap: 12, marginTop: 4, marginBottom: 8 },
  statCard: { width: "48%", borderWidth: 1, borderRadius: 16, padding: 14, gap: 6 },
  statTop: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  statLabel: { fontSize: 12, flex: 1, flexWrap: "wrap", paddingRight: 4, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular },
  statIconWrap: { width: 36, height: 36, borderRadius: 12, alignItems: "center", justifyContent: "center" },
  permitCard: {
    borderWidth: 1,
    borderRadius: 18,
    padding: 14,
    marginBottom: 12,
    overflow: "hidden",
  },
  permitTopRow: { flexDirection: "row", alignItems: "center", gap: 12 },
  permitIconBox: { width: 42, height: 42, borderRadius: 14, alignItems: "center", justifyContent: "center" },
  permitTextBlock: { flex: 1, gap: 6 },
  cardHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    gap: 10,
  },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  permitSub: {
    fontSize: 12,
    color: "#6B7280",
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
  },
  cardDetail: {
    fontSize: 12,
    color: "#6B7280",
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular
  }
});
