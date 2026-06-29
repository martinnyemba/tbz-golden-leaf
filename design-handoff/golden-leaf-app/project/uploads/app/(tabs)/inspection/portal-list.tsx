import AsyncStorage from "@react-native-async-storage/async-storage";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { useFocusEffect, useLocalSearchParams, useRouter } from "expo-router";
import { useCallback, useMemo, useState } from "react";
import {
  Alert,
  FlatList,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  TextInput,
  View,
} from "react-native";

import { LeafLoader } from "@/components/LeafLoader";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { readCacheItems, writeCache } from "@/lib/offline-cache";
import { apiErrorUi, apiFetchJson } from "@/lib/inspection-storage";

const API_BASE_URL_KEY = "tbz:portalBaseUrl:v1";
const AUTH_ACCESS_KEY = "tbz:portalAccessToken:v1";
const AUTH_REFRESH_KEY = "tbz:portalRefreshToken:v1";

function normalizeBaseUrl(raw: string) {
  return raw.trim().replace(/\/+$/, "");
}

function sanitizePortalBaseUrl(raw: string) {
  const value = normalizeBaseUrl(raw);
  if (!value) return "";
  try {
    const url = new URL(value);
    return normalizeBaseUrl(`${url.protocol}//${url.host}`);
  } catch {
    try {
      const needsHttp =
        value.startsWith("localhost") ||
        value.startsWith("127.0.0.1") ||
        /^\d{1,3}(\.\d{1,3}){3}(:\d+)?$/.test(value);
      const url = new URL(`${needsHttp ? "http" : "https"}://${value}`);
      return normalizeBaseUrl(`${url.protocol}//${url.host}`);
    } catch {
      return value;
    }
  }
}

function runtimePortalBaseUrl(raw: string) {
  const baseUrl = sanitizePortalBaseUrl(raw);
  if (!baseUrl) return "";
  try {
    const url = new URL(baseUrl);
    const host = url.hostname;
    if (Platform.OS === "android" && (host === "localhost" || host === "127.0.0.1")) {
      url.hostname = "10.0.2.2";
      return normalizeBaseUrl(url.toString());
    }
    return normalizeBaseUrl(url.toString());
  } catch {
    return baseUrl;
  }
}

type PortalInspection = {
  id: string;
  grower_name?: string;
  grower_tbz_id?: string | null;
  inspector_name?: string;
  inspection_type_display?: string;
  inspection_type?: string;
  scheduled_date?: string;
  status?: string;
  province?: string;
  district?: string;
};

type ApiListResponse<T> = { results?: T[] } | T[];

function listFromApi<T>(payload: ApiListResponse<T>) {
  return Array.isArray(payload) ? payload : payload.results ?? [];
}

export default function PortalInspectionListScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const params = useLocalSearchParams<{ status?: string; title?: string; type?: string }>();

  const status = (params.status ?? "").toString();
  const title = (params.title ?? "Inspections").toString();
  const type = (params.type ?? "").toString();

  const [items, setItems] = useState<PortalInspection[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState("");

  const typePills = useMemo(() => {
    return [
      { key: "", label: "All Types" },
      { key: "GROWER_VALIDATION", label: "Grower Validation" },
      { key: "NURSERY_INSPECTION", label: "Nursery" },
      { key: "FIELD_INSPECTION", label: "Field" },
      { key: "CURING_INSPECTION", label: "Curing" },
    ];
  }, []);

  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      const cached = await readCacheItems<PortalInspection>("inspections");
      if (cached.length > 0 && items.length === 0) {
        const q = search.trim().toLowerCase();
        const filtered = cached.filter((x) => {
          if (status && String(x.status ?? "").toUpperCase() !== status.toUpperCase()) return false;
          if (type && String(x.inspection_type ?? "").toUpperCase() !== type.toUpperCase()) return false;
          if (!q) return true;
          const hay = `${x.grower_name ?? ""} ${x.grower_tbz_id ?? ""} ${x.inspector_name ?? ""}`.toLowerCase();
          return hay.includes(q);
        });
        setItems(filtered);
      }

      const qs = new URLSearchParams();
      if (status) qs.set("status", status);
      if (search.trim()) qs.set("search", search.trim());
      if (type) qs.set("inspection_type", type);

      const apiPath = `/api/v1/inspectorate/inspections/${qs.toString() ? `?${qs.toString()}` : ""}`;
      const resp = await apiFetchJson(apiPath, { method: "GET" });
      if (!resp.ok) {
        const ui = apiErrorUi(resp.status, resp.body);
        if (ui.kind === "login_required") {
          router.push({
            pathname: "/portal-login",
            params: { returnTo: `/inspection/portal-list?status=${encodeURIComponent(status)}&title=${encodeURIComponent(title)}` },
          } as any);
          return;
        }
        Alert.alert(ui.title, ui.message);
        return;
      }

      const payload = JSON.parse(resp.body) as ApiListResponse<PortalInspection>;
      const serverItems = listFromApi(payload);
      
      setItems(serverItems);
      if (!status && !type && !search.trim()) void writeCache("inspections", serverItems);
      
      
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Network request failed";
      Alert.alert("Load failed", msg);
    } finally {
      setLoading(false);
    }
  }, [items.length, router, search, status, title, type]);

  useFocusEffect(
    useCallback(() => {
      fetchList();
    }, [fetchList]),
  );

  const headerSubtitle = useMemo(() => {
    if (!status) return "All portal inspections";
    return `Status: ${status}`;
  }, [status]);

  const statusChip = useMemo(() => {
    const s = status.toUpperCase();
    if (s === "SCHEDULED") return { bg: "#E8F3EE", fg: "#0B6B3A", label: "Scheduled" };
    if (s === "IN_PROGRESS") return { bg: "#FEF3C7", fg: "#92400E", label: "In Progress" };
    if (s === "COMPLETED") return { bg: "#DCFCE7", fg: "#166534", label: "Completed" };
    if (s === "CANCELLED") return { bg: "#F3F4F6", fg: "#374151", label: "Cancelled" };
    return { bg: "#F3F4F6", fg: "#374151", label: s || "All" };
  }, [status]);

  const typeChip = useMemo(() => {
    const t = type.toUpperCase();
    if (t === "GROWER_VALIDATION") return { bg: "#DBEAFE", fg: "#1D4ED8", label: "Grower Validation" };
    if (t === "NURSERY_INSPECTION") return { bg: "#DCFCE7", fg: "#166534", label: "Nursery" };
    if (t === "FIELD_INSPECTION") return { bg: "#E0F2FE", fg: "#075985", label: "Field" };
    if (t === "CURING_INSPECTION") return { bg: "#FEF3C7", fg: "#92400E", label: "Curing" };
    return { bg: "#F3F4F6", fg: "#374151", label: "All Types" };
  }, [type]);

  return (
    <ThemedView style={styles.container}>
      <View style={styles.header}>
        <View style={styles.chipsRow}>
          <View style={[styles.chip, { backgroundColor: statusChip.bg }]}>
            <ThemedText style={[styles.chipText, { color: statusChip.fg }]}>{statusChip.label}</ThemedText>
          </View>
          <View style={[styles.chip, { backgroundColor: typeChip.bg }]}>
            <ThemedText style={[styles.chipText, { color: typeChip.fg }]}>{typeChip.label}</ThemedText>
          </View>
        </View>

        <ThemedText style={styles.sub} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
          {headerSubtitle}
        </ThemedText>

        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.pillsRow}>
          {typePills.map((p) => {
            const active = (p.key || "") === (type || "");
            return (
              <Pressable
                key={p.key || "ALL"}
                onPress={() => {
                  router.setParams({ type: p.key || undefined } as any);
                }}
                style={[
                  styles.pill,
                  {
                    backgroundColor: active ? Colors[theme].primary : Colors[theme].surface,
                    borderColor: Colors[theme].border,
                  },
                ]}
              >
                <ThemedText style={{ color: active ? Colors[theme].surface : Colors[theme].text, fontSize: 13 }}>
                  {p.label}
                </ThemedText>
              </Pressable>
            );
          })}
        </ScrollView>

        <View style={[styles.searchWrap, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <MaterialIcons name="search" size={18} color={Colors[theme].muted} />
          <TextInput
            value={search}
            onChangeText={setSearch}
            placeholder="Search grower"
            placeholderTextColor={Colors[theme].muted}
            style={[styles.searchInput, { color: Colors[theme].text }]}
            onSubmitEditing={fetchList}
            returnKeyType="search"
          />
          {loading ? (
            <View style={styles.refreshBtnTop}>
              <LeafLoader size={20} />
            </View>
          ) : (
            <Pressable onPress={fetchList} style={styles.refreshBtnTop}>
              <MaterialIcons name="refresh" size={20} color={Colors[theme].primary} />
            </Pressable>
          )}
        </View>
      </View>

      <FlatList
        data={items}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <Pressable
            onPress={() => router.push({ pathname: "/inspection/detail", params: { id: item.id } } as any)}
            style={[
              styles.card,
              { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface },
            ]}
          >
            <View style={styles.cardTop}>
              <ThemedText type="defaultSemiBold" numberOfLines={1} ellipsizeMode="tail" style={{ flexShrink: 1 }}>
                {item.grower_name ?? "-"}
              </ThemedText>
              <View style={[styles.statusPill, { backgroundColor: "#F3F4F6" }]}>
                <ThemedText style={styles.statusPillText} numberOfLines={1} ellipsizeMode="tail">
                  {item.status ?? "-"}
                </ThemedText>
              </View>
            </View>
            <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
              {item.grower_tbz_id ?? "—"} • {item.inspector_name ?? "—"}
            </ThemedText>
            <View style={styles.cardBottom}>
              <ThemedText style={[styles.small, { flexShrink: 1 }]} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted} numberOfLines={1} ellipsizeMode="tail">
                {item.inspection_type_display ?? "Inspection"}
              </ThemedText>
              <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted} numberOfLines={1} ellipsizeMode="tail">
                {item.scheduled_date ?? "—"}
              </ThemedText>
            </View>
            <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted} numberOfLines={1} ellipsizeMode="tail">
              {item.province ?? "—"} / {item.district ?? "—"}
            </ThemedText>
          </Pressable>
        )}
        ListEmptyComponent={
          <View style={[styles.empty, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <ThemedText type="defaultSemiBold">No inspections</ThemedText>
            <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
              Try changing filters or search.
            </ThemedText>
          </View>
        }
      />
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { padding: 20, gap: 10 },
  headerRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  headerTitle: { fontSize: 18, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  sub: { lineHeight: 18 },
  chipsRow: { flexDirection: "row", gap: 8, flexWrap: "wrap" },
  chip: { paddingHorizontal: 10, paddingVertical: 6, borderRadius: 999 },
  chipText: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  pillsRow: { gap: 8, paddingVertical: 4 },
  pill: { borderWidth: 1, borderRadius: 999, paddingHorizontal: 12, paddingVertical: 8 },
  searchWrap: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  searchInput: { flex: 1, fontSize: 14, paddingVertical: 0 },
  refreshBtnTop: { paddingHorizontal: 6, paddingVertical: 6 },
  list: { paddingHorizontal: 20, paddingBottom: 20, gap: 12 },
  card: { borderWidth: 1, borderRadius: 16, padding: 14, gap: 6, overflow: "hidden" },
  cardTop: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", gap: 10 },
  cardBottom: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", gap: 10 },
  statusPill: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 999 },
  statusPillText: { fontSize: 12, color: "#374151" },
  small: { fontSize: 12, lineHeight: 16 },
  empty: { borderWidth: 1, borderRadius: 16, padding: 14, gap: 6, overflow: "hidden" },
});
