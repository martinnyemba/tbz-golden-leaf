import React, { useCallback, useEffect, useState } from "react";
import { Alert, Pressable, ScrollView, StyleSheet, View } from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { TextField, SelectField, PrimaryButton } from "@/components/ui/form-controls";
import {
  extractErrorMessage,
  getPendingSaleCaptures,
  retrySaleCapture,
  updatePendingSaleCapture,
  type PendingSaleCapture,
} from "@/lib/inspection-storage";
import { apiFetchJson } from "@/lib/inspection-storage";
import { CONFLICT_UI } from "@/lib/sync-conflict";
import { useNetworkStatus } from "@/lib/network-state";
import { FontFamily, FontWeight } from "@/constants/typography";

export default function EditPendingSaleScreen() {
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id: string }>();
  const { isConnected } = useNetworkStatus();
  const isOffline = isConnected === false;

  const [sale, setSale] = useState<PendingSaleCapture | null>(null);
  const [salesfloors, setSalesfloors] = useState<any[]>([]);
  const [buyers, setBuyers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  // Editable fields
  const [salesfloorId, setSalesfloorId] = useState("");
  const [buyerId, setBuyerId] = useState("");
  const [season, setSeason] = useState("");
  const [saleDate, setSaleDate] = useState("");
  const [growerRepName, setGrowerRepName] = useState("");

  const [isSaving, setIsSaving] = useState(false);

  const safeJson = useCallback((raw: string) => {
    try { return JSON.parse(raw); } catch { return null; }
  }, []);

  const salesfloorLabel = (sf: any) =>
    String(sf?.name ?? sf?.legal_name ?? sf?.company_name ?? "").trim();

  const buyerLabel = (b: any) =>
    String(b?.company_name ?? b?.name ?? b?.legal_name ?? "").trim();

  async function fetchEntities(entityType: string) {
    const collected: any[] = [];
    let nextPath: string | null =
      `/api/v1/entities/legal-entities/?entity_type=${encodeURIComponent(entityType)}&page_size=250`;
    while (nextPath) {
      const resp = await apiFetchJson(nextPath, { method: "GET" });
      if (!resp.ok) break;
      const payload = safeJson(resp.body);
      if (!payload) break;
      const rows = Array.isArray(payload) ? payload : (payload.results ?? []);
      collected.push(...rows);
      const nextRaw = Array.isArray(payload) ? null : (payload.next ?? null);
      if (!nextRaw || typeof nextRaw !== "string") { nextPath = null; continue; }
      try {
        const u = new URL(nextRaw);
        nextPath = `${u.pathname}${u.search}`;
      } catch { nextPath = nextRaw.startsWith("/") ? nextRaw : `/${nextRaw}`; }
    }
    return Array.from(new Map(collected.map((x) => [String(x.id), x])).values());
  }

  useEffect(() => {
    async function init() {
      setLoading(true);
      try {
        const all = await getPendingSaleCaptures();
        const found = all.find((s) => s.id === id) ?? null;
        setSale(found);
        if (!found) return;

        // Populate edit fields from the existing record
        const firstBale = found.payload.bales[0];
        setSalesfloorId(found.payload.salesfloor ?? "");
        setBuyerId(firstBale?.buyer ?? "");
        setSeason(firstBale?.season ?? found.payload.season ?? "");
        setSaleDate(firstBale?.sale_date ?? "");
        setGrowerRepName(firstBale?.grower_rep_name ?? "");

        // Load lookup data from cache (works offline)
        const [sfs, bys] = await Promise.all([
          fetchEntities("SALES_FLOOR"),
          fetchEntities("BUYER"),
        ]);
        setSalesfloors(sfs.filter((s) => s?.is_active !== false));
        setBuyers(bys.filter((b) => b?.is_active !== false).map((b) => ({ ...b, company_name: b.company_name ?? b.name })));
      } finally {
        setLoading(false);
      }
    }
    void init();
  }, [id]);

  const sfOptions = salesfloors
    .map((sf) => ({ label: salesfloorLabel(sf), value: String(sf?.id ?? "") }))
    .filter((o) => o.label && o.value);

  const buyerOptions = [
    { label: "— No buyer —", value: "" },
    ...buyers
      .map((b) => ({ label: buyerLabel(b), value: String(b?.id ?? "") }))
      .filter((o) => o.label && o.value),
  ];

  const handleSaveAndRetry = async () => {
    if (!season.trim() || !saleDate.trim()) {
      Alert.alert("Missing Fields", "Season and sale date are required.");
      return;
    }

    const sfMatch = salesfloors.find((sf) => String(sf?.id ?? "") === salesfloorId);
    const buyerMatch = buyers.find((b) => String(b?.id ?? "") === buyerId);

    setIsSaving(true);
    try {
      await updatePendingSaleCapture(id, {
        salesfloor: sfMatch ? String(sfMatch.id) : undefined,
        salesfloorName: sfMatch ? salesfloorLabel(sfMatch) : undefined,
        buyer: buyerId || undefined,
        buyerName: buyerMatch ? buyerLabel(buyerMatch) : undefined,
        season: season.trim(),
        sale_date: saleDate.trim(),
        grower_rep_name: growerRepName.trim() || undefined,
      });

      if (!isOffline) {
        const { success, conflictDetail } = await retrySaleCapture(id);
        if (success) {
          Alert.alert("Success", "Sale batch submitted successfully.", [
            { text: "OK", onPress: () => router.back() },
          ]);
          return;
        }
        if (conflictDetail) {
          Alert.alert(
            conflictDetail.summary,
            `${conflictDetail.advice}\n\nThe updated record has been saved. You can retry again later.`,
            [{ text: "OK", onPress: () => router.back() }],
          );
          return;
        }
      }

      Alert.alert(
        isOffline ? "Saved" : "Saved — retry later",
        isOffline
          ? "Changes saved. The record will be submitted when you reconnect."
          : "Changes saved. Tap Retry on the queue screen to submit.",
        [{ text: "OK", onPress: () => router.back() }],
      );
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to save changes.";
      Alert.alert("Error", extractErrorMessage(msg) || "Failed to save changes.");
    } finally {
      setIsSaving(false);
    }
  };

  if (loading) {
    return (
      <ThemedView style={styles.container}>
        <View style={styles.header}>
          <Pressable onPress={() => router.back()} style={{ padding: 8, marginLeft: -8, marginRight: 8 }}>
            <MaterialIcons name="arrow-back" size={26} color="#111827" />
          </Pressable>
          <ThemedText type="subtitle" style={{ fontSize: 16 }}>Edit Pending Sale</ThemedText>
        </View>
        <View style={styles.centred}>
          <ThemedText style={{ color: "#6B7280" }}>Loading…</ThemedText>
        </View>
      </ThemedView>
    );
  }

  if (!sale) {
    return (
      <ThemedView style={styles.container}>
        <View style={styles.header}>
          <Pressable onPress={() => router.back()} style={{ padding: 8, marginLeft: -8, marginRight: 8 }}>
            <MaterialIcons name="arrow-back" size={26} color="#111827" />
          </Pressable>
          <ThemedText type="subtitle" style={{ fontSize: 16 }}>Edit Pending Sale</ThemedText>
        </View>
        <View style={styles.centred}>
          <ThemedText style={{ color: "#DC2626" }}>Record not found.</ThemedText>
        </View>
      </ThemedView>
    );
  }

  const conflict = sale.conflictDetail;

  return (
    <ThemedView style={styles.container}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={{ padding: 8, marginLeft: -8, marginRight: 8 }}>
          <MaterialIcons name="arrow-back" size={26} color="#111827" />
        </Pressable>
        <View style={{ flex: 1 }}>
          <ThemedText type="subtitle" style={{ fontSize: 16 }}>Edit Pending Sale</ThemedText>
          <ThemedText style={{ fontSize: 12, color: "#6B7280" }}>
            {sale.meta.baleCount} bales · {sale.meta.growerName ?? "Unknown grower"}
          </ThemedText>
        </View>
      </View>

      {isOffline && (
        <View style={styles.offlineBanner}>
          <MaterialIcons name="wifi-off" size={14} color="#FFFFFF" />
          <ThemedText style={styles.offlineBannerText}>Offline — changes will be saved locally</ThemedText>
        </View>
      )}

      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {/* Conflict guidance card */}
        {conflict && (
          <View style={[styles.conflictCard, { backgroundColor: CONFLICT_UI[conflict.category].bg, borderColor: CONFLICT_UI[conflict.category].border }]}>
            <View style={styles.conflictCardHeader}>
              <MaterialIcons name={CONFLICT_UI[conflict.category].icon as any} size={16} color={CONFLICT_UI[conflict.category].text} />
              <ThemedText style={[styles.conflictTitle, { color: CONFLICT_UI[conflict.category].text }]}>
                {conflict.summary}
              </ThemedText>
            </View>
            <ThemedText style={[styles.conflictAdvice, { color: CONFLICT_UI[conflict.category].text }]}>
              {conflict.advice}
            </ThemedText>
          </View>
        )}

        {/* Immutable context */}
        <View style={styles.section}>
          <ThemedText style={styles.sectionTitle}>Batch Context (read-only)</ThemedText>
          <View style={styles.readonlyGrid}>
            <ReadonlyRow label="Grower" value={sale.meta.growerName ?? "—"} />
            <ReadonlyRow label="Permit" value={sale.meta.permitNumber ?? "—"} />
            <ReadonlyRow label="Bale count" value={String(sale.meta.baleCount)} />
            <ReadonlyRow label="Tobacco type" value={sale.payload.bales[0]?.tobacco_type ?? "—"} />
          </View>
          <ThemedText style={styles.baleListNote}>
            Bale tickets and grades cannot be changed after capture.
          </ThemedText>
        </View>

        {/* Editable fields */}
        <View style={styles.section}>
          <ThemedText style={styles.sectionTitle}>Editable Fields</ThemedText>
          <SelectField
            label="Sales Floor *"
            value={salesfloorId}
            onChange={setSalesfloorId}
            options={sfOptions}
          />
          <SelectField
            label="Buyer / Company"
            value={buyerId}
            onChange={setBuyerId}
            options={buyerOptions}
          />
          <TextField label="Season *" value={season} onChangeText={setSeason} placeholder="2025/2026" />
          <TextField label="Sale Date * (YYYY-MM-DD)" value={saleDate} onChangeText={setSaleDate} placeholder="2025-06-01" />
          <TextField label="Grower Representative" value={growerRepName} onChangeText={setGrowerRepName} />
        </View>

        {/* Bale preview (read-only) */}
        <View style={styles.section}>
          <ThemedText style={styles.sectionTitle}>Captured Bales</ThemedText>
          <View style={styles.baleTable}>
            <View style={styles.baleTableHeader}>
              <ThemedText style={[styles.baleCol, styles.baleHeaderText]}>Ticket</ThemedText>
              <ThemedText style={[styles.baleCol, styles.baleHeaderText]}>Grade</ThemedText>
              <ThemedText style={[styles.baleCol, styles.baleHeaderText]}>Wt (kg)</ThemedText>
              <ThemedText style={[styles.baleCol, styles.baleHeaderText]}>Status</ThemedText>
            </View>
            {sale.payload.bales.map((b, i) => (
              <View key={i} style={[styles.baleTableRow, i % 2 === 1 && styles.baleTableRowAlt]}>
                <ThemedText style={[styles.baleCol, styles.baleCell]} numberOfLines={1}>{b.bale_ticket_number}</ThemedText>
                <ThemedText style={[styles.baleCol, styles.baleCell]}>{b.grade_mark}</ThemedText>
                <ThemedText style={[styles.baleCol, styles.baleCell]}>{b.weight_kg}</ThemedText>
                <ThemedText style={[styles.baleCol, styles.baleCell, { color: b.status === "BOUGHT" ? "#0B6B3A" : "#DC2626" }]}>{b.status}</ThemedText>
              </View>
            ))}
          </View>
        </View>

        <PrimaryButton
          title={isSaving ? "Saving…" : isOffline ? "Save Changes" : "Save & Retry Submission"}
          onPress={handleSaveAndRetry}
          disabled={isSaving}
        />
      </ScrollView>
    </ThemedView>
  );
}

function ReadonlyRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.readonlyRow}>
      <ThemedText style={styles.readonlyLabel}>{label}</ThemedText>
      <ThemedText style={styles.readonlyValue} numberOfLines={1}>{value}</ThemedText>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#F9FAFB" },
  header: { flexDirection: "row", alignItems: "center", paddingHorizontal: 20, paddingTop: 60, paddingBottom: 16, backgroundColor: "#FFFFFF", borderBottomWidth: 1, borderBottomColor: "#E5E7EB" },
  offlineBanner: { flexDirection: "row", alignItems: "center", gap: 8, backgroundColor: "#374151", paddingHorizontal: 16, paddingVertical: 8 },
  offlineBannerText: { color: "#FFFFFF", fontSize: 12, flex: 1 },
  centred: { flex: 1, alignItems: "center", justifyContent: "center" },
  content: { padding: 16, paddingBottom: 60, gap: 16 },

  conflictCard: { borderRadius: 10, padding: 14, borderWidth: 1, gap: 6 },
  conflictCardHeader: { flexDirection: "row", alignItems: "center", gap: 6 },
  conflictTitle: { fontSize: 13, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  conflictAdvice: { fontSize: 12, lineHeight: 18 },

  section: { backgroundColor: "#FFFFFF", borderRadius: 12, padding: 16, borderWidth: 1, borderColor: "#E5E7EB", gap: 10 },
  sectionTitle: { fontSize: 13, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#374151", marginBottom: 4 },

  readonlyGrid: { gap: 6 },
  readonlyRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingVertical: 4, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: "#F3F4F6" },
  readonlyLabel: { fontSize: 12, color: "#6B7280" },
  readonlyValue: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, color: "#111827", maxWidth: "60%" },
  baleListNote: { fontSize: 11, color: "#9CA3AF", marginTop: 4 },

  baleTable: { borderRadius: 8, borderWidth: 1, borderColor: "#E5E7EB", overflow: "hidden" },
  baleTableHeader: { flexDirection: "row", backgroundColor: "#F9FAFB", paddingVertical: 8, paddingHorizontal: 10, borderBottomWidth: 1, borderBottomColor: "#E5E7EB" },
  baleHeaderText: { fontSize: 10, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#6B7280", textTransform: "uppercase" },
  baleTableRow: { flexDirection: "row", paddingVertical: 8, paddingHorizontal: 10 },
  baleTableRowAlt: { backgroundColor: "#F9FAFB" },
  baleCol: { flex: 1 },
  baleCell: { fontSize: 12, color: "#111827" },
});
