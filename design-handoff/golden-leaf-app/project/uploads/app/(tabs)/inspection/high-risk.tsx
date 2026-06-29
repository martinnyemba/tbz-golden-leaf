import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { useFocusEffect, useRouter } from "expo-router";
import { useCallback, useState } from "react";
import {
  Alert,
  FlatList,
  Pressable,
  StyleSheet,
  TextInput,
  View,
} from "react-native";

import { LeafLoader } from "@/components/LeafLoader";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiFetchJson } from "@/lib/inspection-storage";

type PortalValidation = {
  id: string;
  grower_name?: string;
  nrc_number?: string;
  province?: string;
  district?: string;
  risk_score?: number;
  created_at?: string;
};

type ApiListResponse<T> = { results?: T[] } | T[];

function listFromApi<T>(payload: ApiListResponse<T>) {
  return Array.isArray(payload) ? payload : payload.results ?? [];
}

export default function HighRiskGrowersScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  const [items, setItems] = useState<PortalValidation[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState("");

  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      const qs = new URLSearchParams();
      if (search.trim()) qs.set("search", search.trim());
      const apiPath = `/api/v1/inspectorate/validations/high-risk/${qs.toString() ? `?${qs.toString()}` : ""}`;
      const resp = await apiFetchJson(apiPath, { method: "GET" });
      if (!resp.ok) {
        if (resp.status === 0) {
          router.push({ pathname: "/portal-login", params: { returnTo: "/inspection/high-risk" } } as any);
        }
        return;
      }

      const payload = JSON.parse(resp.body) as ApiListResponse<PortalValidation>;
      setItems(listFromApi(payload));
    } catch {
    } finally {
      setLoading(false);
    }
  }, [router, search]);

  useFocusEffect(
    useCallback(() => {
      fetchList();
    }, [fetchList]),
  );

  return (
    <ThemedView style={styles.container}>
      <View style={styles.header}>
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
            <View style={styles.refreshBtn}>
              <LeafLoader size={20} />
            </View>
          ) : (
            <Pressable onPress={fetchList} style={styles.refreshBtn}>
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
            onPress={() => {
              Alert.alert(
                "High Risk Validation",
                [
                  `Grower: ${item.grower_name ?? "-"}`,
                  `NRC: ${item.nrc_number ?? "-"}`,
                  item.province ? `Province: ${item.province}` : "",
                  item.district ? `District: ${item.district}` : "",
                  item.risk_score !== undefined ? `Risk score: ${item.risk_score}` : "",
                  item.created_at ? `Created: ${item.created_at}` : "",
                ]
                  .filter(Boolean)
                  .join("\n"),
              );
            }}
            style={[
              styles.card,
              { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface },
            ]}
          >
            <View style={styles.cardTop}>
              <ThemedText type="defaultSemiBold" numberOfLines={1} ellipsizeMode="tail" style={{ flexShrink: 1 }}>
                {item.grower_name ?? "Grower"}
              </ThemedText>
              <ThemedText style={{ color: "#DC2626" }} type="defaultSemiBold" numberOfLines={1} ellipsizeMode="tail">
                {item.risk_score ?? 0}
              </ThemedText>
            </View>
            <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted} numberOfLines={1} ellipsizeMode="tail">
              {item.nrc_number ?? "-"} • {item.province ?? "-"} / {item.district ?? "-"}
            </ThemedText>
          </Pressable>
        )}
        ListEmptyComponent={
          <View style={[styles.empty, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <ThemedText type="defaultSemiBold">No high risk growers</ThemedText>
            <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
              Risk score threshold is 70+.
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
  refreshBtn: { paddingHorizontal: 4, paddingVertical: 4 },
  list: { paddingHorizontal: 20, paddingBottom: 20, gap: 12 },
  card: { borderWidth: 1, borderRadius: 16, padding: 14, gap: 6, overflow: "hidden" },
  cardTop: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", gap: 10 },
  small: { fontSize: 12, lineHeight: 16 },
  empty: { borderWidth: 1, borderRadius: 16, padding: 14, gap: 6, overflow: "hidden" },
});
