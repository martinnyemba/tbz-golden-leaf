import { useCallback, useState } from "react";
import { Alert, StyleSheet, View, ScrollView, Pressable } from "react-native";
import { useFocusEffect, useLocalSearchParams, useRouter } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

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

export default function PermitListScreen() {
  const router = useRouter();
  const { filter = "ALL" } = useLocalSearchParams<{ filter: "ALL" | "ACTIVE" | "PENDING" | "EXPIRED" }>();
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
            { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: `/permit-list?filter=${encodeURIComponent(String(filter))}` } } as any) },
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
  }, [fetchAllPermits, filter, permits.length, router]);

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
    toDate.setHours(0,0,0,0);
    today.setHours(0,0,0,0);
    return toDate >= today;
  };

  const isExpired = (p: TransportPermitApi) => {
    if (p.status === "EXPIRED") return true;
    if (p.status === "APPROVED" && p.valid_to) {
      const toDate = new Date(p.valid_to);
      const today = new Date();
      toDate.setHours(0,0,0,0);
      today.setHours(0,0,0,0);
      return toDate < today;
    }
    return false;
  };

  const displayedPermits = permits.filter(p => {
    if (filter === "ALL") return true;
    if (filter === "ACTIVE") return isActive(p);
    if (filter === "PENDING") return p.status === "PENDING";
    if (filter === "EXPIRED") return isExpired(p);
    return true;
  });

  return (
    <ThemedView style={styles.container}>
      <View style={styles.headerRow}>
        <MaterialIcons name="arrow-back" size={24} color={Colors[theme].text} onPress={() => router.back()} style={{ marginRight: 16 }} />
        <ThemedText type="subtitle" numberOfLines={1} ellipsizeMode="tail" style={{ flexShrink: 1 }}>
          {filter === "ALL" ? "Total Requested" : filter === "ACTIVE" ? "Active" : filter === "PENDING" ? "Pending" : "Expired"} Permits
        </ThemedText>
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        {loading ? (
          <View style={{ marginTop: 30, alignItems: "center" }}>
            <LeafLoader size={42} />
          </View>
        ) : displayedPermits.length === 0 ? (
          <View style={{ alignItems: "center", paddingVertical: 40, opacity: 0.6 }}>
            <MaterialIcons name="folder-open" size={48} color={Colors[theme].muted} />
            <ThemedText style={{ marginTop: 12 }}>No transport permits found.</ThemedText>
          </View>
        ) : (
          displayedPermits.map(permit => (
            <Pressable 
              key={permit.id} 
              onPress={() => router.push({ pathname: "/permit-detail", params: { id: permit.id } })}
              android_ripple={{ color: 'rgba(0, 0, 0, 0.1)', borderless: false }}
              style={[styles.permitCard, { backgroundColor: Colors[theme].surface }]}
            >
              <View style={styles.cardHeader}>
                <ThemedText type="defaultSemiBold" style={{ flex: 1, flexShrink: 1 }} numberOfLines={1} ellipsizeMode="tail">
                  {permit.permit_number || "PENDING ISSUANCE"}
                </ThemedText>
                <View style={[styles.statusBadge, { 
                  backgroundColor: permit.status === "APPROVED" ? "#CFEDE1" : permit.status === "PENDING" ? "#FEF3C7" : "#FEE2E2" 
                }]}>
                  <ThemedText style={{ 
                    fontSize: 10, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold,
                    color: permit.status === "APPROVED" ? "#0B6B3A" : permit.status === "PENDING" ? "#D97706" : "#B91C1C" 
                  }} numberOfLines={1} ellipsizeMode="tail">
                    {permit.status}
                  </ThemedText>
                </View>
              </View>
              <ThemedText style={{ fontSize: 13, marginBottom: 6, flexShrink: 1 }} lightColor="#4B5563" darkColor="#9CA3AF" numberOfLines={1} ellipsizeMode="tail">
                Grower:{" "}
                <ThemedText type="defaultSemiBold" style={{ fontSize: 13 }} numberOfLines={1} ellipsizeMode="tail">
                  {permit.grower_name}
                </ThemedText>
              </ThemedText>
              <View style={{ flexDirection: "row", gap: 12, marginBottom: 2 }}>
                <ThemedText style={[styles.cardDetail, { flex: 1, flexShrink: 1 }]} numberOfLines={1} ellipsizeMode="tail">
                  Origin: {permit.origin_district}
                </ThemedText>
                <ThemedText style={[styles.cardDetail, { flex: 1, flexShrink: 1, textAlign: "right" }]} numberOfLines={1} ellipsizeMode="tail">
                  Salesfloor: {permit.destination_salesfloor}
                </ThemedText>
              </View>
              <View style={{ flexDirection: "row", gap: 12 }}>
                <ThemedText style={[styles.cardDetail, { flex: 1, flexShrink: 1 }]} numberOfLines={1} ellipsizeMode="tail">
                  Bales: {permit.total_bales}
                </ThemedText>
                <ThemedText style={[styles.cardDetail, { flex: 1, flexShrink: 1, textAlign: "right" }]} numberOfLines={1} ellipsizeMode="tail">
                  Weight: {permit.total_weight_kg} kg
                </ThemedText>
              </View>
            </Pressable>
          ))
        )}
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  headerRow: { flexDirection: "row", alignItems: "center", paddingHorizontal: 20, paddingTop: 16, paddingBottom: 8 },
  content: { padding: 20, paddingBottom: 40 },
  permitCard: {
    borderRadius: 16,
    padding: 16,
    marginBottom: 16,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 3,
    marginHorizontal: 4,
    overflow: "hidden",
  },
  cardHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8,
  },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  cardDetail: {
    fontSize: 12,
    color: "#6B7280",
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular
  }
});
