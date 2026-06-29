import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { Stack, useFocusEffect, useLocalSearchParams, useRouter } from "expo-router";
import React, { useCallback, useMemo, useState } from "react";
import { Alert, ScrollView, StyleSheet, View } from "react-native";

import { LeafLoader } from "@/components/LeafLoader";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton } from "@/components/ui/form-controls";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiErrorUi, apiFetchJson, extractErrorMessage } from "@/lib/inspection-storage";

type GroupPermitEntry = {
  id: string;
  grower_name?: string;
  grower_tbz_id?: string;
  grower_category?: string;
  total_bales?: number;
  total_weight_kg?: string | number;
  remaining_bales?: number;
  remaining_weight_kg?: string | number;
  status?: string;
};

type GroupPermit = {
  id: string;
  group_permit_number?: string | null;
  license_plate?: string;
  origin_province?: string;
  origin_district?: string;
  destination_salesfloor?: string;
  purpose?: string;
  valid_from?: string | null;
  valid_to?: string | null;
  total_bales?: number;
  total_weight_kg?: string | number;
  status?: string;
  rejection_reason?: string;
  entries?: GroupPermitEntry[];
  created_at?: string;
};

function formatDate(raw?: string | null) {
  if (!raw) return "—";
  return String(raw).slice(0, 10);
}

function statusBadgeColors(status?: string) {
  const s = String(status || "").toUpperCase();
  if (s === "APPROVED") return { bg: "#E8F3EE", text: "#0B6B3A" };
  if (s === "PENDING") return { bg: "#FEF3C7", text: "#D97706" };
  if (s === "DRAFT") return { bg: "#E5E7EB", text: "#374151" };
  return { bg: "#FEE2E2", text: "#B91C1C" };
}

export default function PermitGroupDetailScreen() {
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  const [gp, setGp] = useState<GroupPermit | null>(null);
  const [entries, setEntries] = useState<GroupPermitEntry[]>([]);
  const [loading, setLoading] = useState(true);

  const title = useMemo(() => {
    const permitNo = gp?.group_permit_number;
    if (permitNo) return permitNo;
    return "Group Permit Details";
  }, [gp?.group_permit_number]);

  const fetchDetails = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const resp = await apiFetchJson(`/api/v1/permits/group-permits/${id}/`, { method: "GET" });
      if (!resp.ok) {
        const ui = apiErrorUi(resp.status, resp.body);
        Alert.alert(ui.title, ui.message);
        return;
      }
      const parsed = JSON.parse(resp.body) as GroupPermit;
      setGp(parsed);

      const entriesResp = await apiFetchJson(`/api/v1/permits/group-permits/${id}/entries/`, { method: "GET" });
      if (entriesResp.ok) {
        const parsedEntries = JSON.parse(entriesResp.body);
        setEntries(Array.isArray(parsedEntries) ? parsedEntries : []);
      } else {
        setEntries([]);
      }
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to load group permit.";
      Alert.alert("Error", extractErrorMessage(String(msg)) || "Failed to load group permit.");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useFocusEffect(
    useCallback(() => {
      void fetchDetails();
    }, [fetchDetails]),
  );

  const badge = statusBadgeColors(gp?.status);
  const subtitle = gp?.license_plate ? `${gp.license_plate} → ${gp?.destination_salesfloor ?? "—"}` : "—";

  if (loading || !gp) {
    return (
      <ThemedView style={styles.container}>
        <Stack.Screen
          options={{
            headerShown: true,
            title: "Group Permit Details",
            headerStyle: { backgroundColor: Colors[theme].primary },
            headerTitleStyle: { fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
            headerTintColor: Colors[theme].surface,
          }}
        />
        <View style={{ marginTop: 50, alignItems: "center" }}>
          <LeafLoader size={42} />
        </View>
      </ThemedView>
    );
  }

  return (
    <ThemedView style={styles.container}>
      <Stack.Screen
        options={{
          headerShown: true,
          title,
          headerStyle: { backgroundColor: Colors[theme].primary },
          headerTitleStyle: { fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
          headerTintColor: Colors[theme].surface,
        }}
      />

      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <View style={[styles.heroCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <View style={styles.heroTop}>
            <View style={styles.heroIconWrap}>
              <MaterialIcons name="local-shipping" size={28} color={Colors[theme].primary} />
            </View>
            <View style={{ flex: 1, marginLeft: 12 }}>
              <ThemedText style={[styles.heroTitle, { color: Colors[theme].text }]} numberOfLines={1} ellipsizeMode="tail">
                {gp.group_permit_number || (gp.status === "DRAFT" ? "Draft Group Permit" : "Group Permit")}
              </ThemedText>
              <ThemedText style={styles.heroSubtitle} numberOfLines={1} ellipsizeMode="tail">
                {subtitle}
              </ThemedText>
            </View>
            <View style={[styles.badge, { backgroundColor: badge.bg }]}>
              <ThemedText style={[styles.badgeText, { color: badge.text }]} numberOfLines={1}>
                {gp.status || "—"}
              </ThemedText>
            </View>
          </View>

          <View style={styles.heroGrid}>
            <View style={styles.heroCell}>
              <ThemedText style={styles.heroLabel}>Origin</ThemedText>
              <ThemedText style={styles.heroValue} numberOfLines={1} ellipsizeMode="tail">
                {(gp.origin_province || "—") + " • " + (gp.origin_district || "—")}
              </ThemedText>
            </View>
            <View style={styles.heroCell}>
              <ThemedText style={styles.heroLabel}>Purpose</ThemedText>
              <ThemedText style={styles.heroValue} numberOfLines={1} ellipsizeMode="tail">
                {gp.purpose || "—"}
              </ThemedText>
            </View>
            <View style={styles.heroCell}>
              <ThemedText style={styles.heroLabel}>Valid From</ThemedText>
              <ThemedText style={styles.heroValue}>{formatDate(gp.valid_from)}</ThemedText>
            </View>
            <View style={styles.heroCell}>
              <ThemedText style={styles.heroLabel}>Valid To</ThemedText>
              <ThemedText style={styles.heroValue}>{formatDate(gp.valid_to)}</ThemedText>
            </View>
            <View style={styles.heroCell}>
              <ThemedText style={styles.heroLabel}>Total Bales</ThemedText>
              <ThemedText style={styles.heroValue}>{String(gp.total_bales ?? 0)}</ThemedText>
            </View>
            <View style={styles.heroCell}>
              <ThemedText style={styles.heroLabel}>Total Weight</ThemedText>
              <ThemedText style={styles.heroValue}>{String(gp.total_weight_kg ?? "0")} kg</ThemedText>
            </View>
          </View>

          {gp.status === "REJECTED" && gp.rejection_reason ? (
            <View style={styles.rejectionBox}>
              <ThemedText style={styles.rejectionTitle}>Rejection Reason</ThemedText>
              <ThemedText style={styles.rejectionText}>{gp.rejection_reason}</ThemedText>
            </View>
          ) : null}
        </View>

        <View style={[styles.sectionCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <View style={styles.sectionHeader}>
            <MaterialIcons name="groups" size={18} color={Colors[theme].primary} style={{ marginRight: 8 }} />
            <ThemedText style={styles.sectionTitle}>Grower Manifest</ThemedText>
            <View style={styles.sectionCountPill}>
              <ThemedText style={styles.sectionCountText}>{String(entries.length)}</ThemedText>
            </View>
          </View>

          {entries.length === 0 ? (
            <View style={{ paddingVertical: 10 }}>
              <ThemedText style={styles.emptyText}>No growers added yet.</ThemedText>
            </View>
          ) : (
            <View style={{ gap: 10 }}>
              {entries.map((e) => (
                <View key={String(e.id)} style={styles.entryRow}>
                  <View style={{ flex: 1 }}>
                    <ThemedText style={styles.entryTitle} numberOfLines={1} ellipsizeMode="tail">
                      {e.grower_name || e.grower_tbz_id || "Grower"}
                    </ThemedText>
                    <ThemedText style={styles.entryMeta} numberOfLines={1} ellipsizeMode="tail">
                      {(e.grower_tbz_id || "—") + (e.grower_category ? ` • ${e.grower_category}` : "")}
                    </ThemedText>
                    <ThemedText style={styles.entryMeta}>
                      {(e.total_bales ?? 0)} bales • {String(e.total_weight_kg ?? "0")} kg
                    </ThemedText>
                    {typeof e.remaining_bales === "number" || e.remaining_weight_kg ? (
                      <ThemedText style={styles.entryMeta}>
                        Remaining: {String(e.remaining_bales ?? "—")} bales • {String(e.remaining_weight_kg ?? "—")} kg
                      </ThemedText>
                    ) : null}
                  </View>
                  {e.status ? (
                    <View style={[styles.entryBadge, { backgroundColor: statusBadgeColors(e.status).bg }]}>
                      <ThemedText style={[styles.entryBadgeText, { color: statusBadgeColors(e.status).text }]} numberOfLines={1}>
                        {String(e.status).toUpperCase()}
                      </ThemedText>
                    </View>
                  ) : null}
                </View>
              ))}
            </View>
          )}
        </View>

        <View style={{ gap: 10 }}>
          <PrimaryButton title="Back to Group Permits" onPress={() => router.back()} />
        </View>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#FFFFFF" },
  content: { padding: 20, paddingBottom: 40, gap: 14 },

  heroCard: { borderWidth: 1, borderRadius: 16, padding: 14 },
  heroTop: { flexDirection: "row", alignItems: "center" },
  heroIconWrap: { width: 44, height: 44, borderRadius: 14, backgroundColor: "#E8F3EE", alignItems: "center", justifyContent: "center" },
  heroTitle: { fontSize: 16, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  heroSubtitle: { marginTop: 2, fontSize: 12, color: "#6B7280" },
  badge: { marginLeft: 10, paddingHorizontal: 10, paddingVertical: 6, borderRadius: 999 },
  badgeText: { fontSize: 10, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },

  heroGrid: { marginTop: 12, flexDirection: "row", flexWrap: "wrap", gap: 10 },
  heroCell: { width: "48%", gap: 2 },
  heroLabel: { fontSize: 11, color: "#6B7280" },
  heroValue: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, color: "#111827" },

  rejectionBox: { marginTop: 12, padding: 12, borderRadius: 12, backgroundColor: "#FEF2F2", borderWidth: 1, borderColor: "#FECACA" },
  rejectionTitle: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#991B1B" },
  rejectionText: { marginTop: 4, fontSize: 12, color: "#991B1B" },

  sectionCard: { borderWidth: 1, borderRadius: 16, padding: 14 },
  sectionHeader: { flexDirection: "row", alignItems: "center", marginBottom: 10 },
  sectionTitle: { fontSize: 14, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#111827" },
  sectionCountPill: { marginLeft: 10, paddingHorizontal: 10, paddingVertical: 4, borderRadius: 999, backgroundColor: "#F3F4F6" },
  sectionCountText: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#111827" },

  emptyText: { fontSize: 12, color: "#6B7280" },

  entryRow: { flexDirection: "row", alignItems: "flex-start", gap: 10, paddingVertical: 10, borderTopWidth: 1, borderTopColor: "#F3F4F6" },
  entryTitle: { fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#111827" },
  entryMeta: { marginTop: 2, fontSize: 12, color: "#6B7280" },
  entryBadge: { paddingHorizontal: 8, paddingVertical: 4, borderRadius: 999 },
  entryBadgeText: { fontSize: 10, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
});

