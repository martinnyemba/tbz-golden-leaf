import AsyncStorage from "@react-native-async-storage/async-storage";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { useFocusEffect, useRouter } from "expo-router";
import { useCallback, useMemo, useState } from "react";
import { Alert, FlatList, Pressable, StyleSheet, View } from "react-native";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from '@/constants/typography';
import { useColorScheme } from "@/hooks/use-color-scheme";
import { listGrowerRegistrations, type LocalGrowerRegistration, apiFetchJson } from "@/lib/inspection-storage";

const DRAFT_KEY = "tbz:growerRegistrationDraft:v1";
const CLEAR_FORM_KEY = "tbz:growerRegistrationClearForm:v1";

function formatWhen(iso: string) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString();
}

function fullName(reg: LocalGrowerRegistration) {
  const g = reg.grower;
  return [g.firstName, g.middleName, g.lastName].filter(Boolean).join(" ").trim();
}

function StatusPill({ status }: { status: LocalGrowerRegistration["syncStatus"] }) {
  const normalized = String(status ?? "").toUpperCase();
  const bg = status === "synced" ? "#E8F3EE" : status === "pending" ? "#FEF3C7" : "#FEE2E2";
  const fg = status === "synced" ? "#0B6B3A" : status === "pending" ? "#D97706" : "#B91C1C";
  return (
    <View style={[styles.statusPill, { backgroundColor: bg }]}>
      <ThemedText style={[styles.statusText, { color: fg }]} numberOfLines={1} ellipsizeMode="tail">
        {normalized}
      </ThemedText>
    </View>
  );
}

function SummaryCard({
  theme,
  icon,
  title,
  value,
  onPress,
  iconBg,
  iconColor,
  valueBg,
  valueColor,
}: {
  theme: "light" | "dark";
  icon: string;
  title: string;
  value: string | number;
  onPress: () => void;
  iconBg: string;
  iconColor: string;
  valueBg: string;
  valueColor: string;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        styles.summaryCard,
        {
          borderColor: Colors[theme].border,
          backgroundColor: Colors[theme].surface,
          opacity: pressed ? 0.92 : 1,
        },
      ]}
    >
      <View style={styles.summaryCardTop}>
        <ThemedText style={styles.summaryTitle} numberOfLines={2} ellipsizeMode="tail">
          {title}
        </ThemedText>
        <View style={[styles.summaryIconBox, { backgroundColor: iconBg }]}>
          <MaterialIcons name={icon as any} size={20} color={iconColor} />
        </View>
      </View>

      <ThemedText style={styles.summarySub} numberOfLines={1} ellipsizeMode="tail">
        Tap to view
      </ThemedText>

      <View style={styles.summaryCardBottom}>
        <View style={[styles.summaryValuePill, { backgroundColor: valueBg }]}>
          <ThemedText style={[styles.summaryValue, { color: valueColor }]} numberOfLines={1} ellipsizeMode="tail">
            {String(value)}
          </ThemedText>
        </View>
        <MaterialIcons name="chevron-right" size={18} color={Colors[theme].muted} />
      </View>
    </Pressable>
  );
}

export default function RegistrationScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const [items, setItems] = useState<LocalGrowerRegistration[]>([]);

  const [portalGrowerCount, setPortalGrowerCount] = useState<number | "-">("-");

  const refresh = useCallback(async () => {
    const all = await listGrowerRegistrations();
    setItems(all);
  }, []);

  const refreshPortalData = useCallback(async () => {
    try {
      const resp = await apiFetchJson("/api/v1/growers/growers/?limit=1", { method: "GET" });
      if (resp.ok) {
        const payload = JSON.parse(resp.body);
        if (payload && typeof payload.count === "number") {
          setPortalGrowerCount(payload.count);
        } else if (Array.isArray(payload)) {
          setPortalGrowerCount(payload.length);
        }
      }
    } catch {
      // ignore
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      refresh();
      refreshPortalData();
    }, [refresh, refreshPortalData]),
  );

  const counts = useMemo(() => {
    const pending = items.filter((r) => r.syncStatus === "pending").length;
    const failed = items.filter((r) => r.syncStatus === "failed").length;
    const synced = items.filter((r) => r.syncStatus === "synced").length;
    return { pending, failed, synced, total: items.length };
  }, [items]);

  return (
    <ThemedView style={styles.container}>
      <View style={styles.header}>
        <View style={styles.topRow}>
          <Pressable
            style={({ pressed }) => [styles.primaryPill, pressed && { opacity: 0.9 }]}
            onPress={async () => {
              await AsyncStorage.removeItem(DRAFT_KEY);
              await AsyncStorage.setItem(CLEAR_FORM_KEY, "1");
              router.push("/registration/new");
            }}
          >
            <MaterialIcons name="person-add" size={18} color={Colors[theme].surface} />
            <ThemedText style={styles.primaryPillText}>Register</ThemedText>
          </Pressable>
        </View>

        <View style={styles.summaryGrid}>
          <SummaryCard
            theme={theme}
            icon="people"
            title="Total Growers"
            value={portalGrowerCount}
            onPress={() => router.push("/registration/growers-list" as any)}
            iconBg="#FFF7E6"
            iconColor={Colors[theme].accent}
            valueBg="#E8F3EE"
            valueColor={Colors[theme].primary}
          />
          <SummaryCard
            theme={theme}
            icon="schedule"
            title="Pending Sync"
            value={counts.pending}
            onPress={() => router.push("/local-registrations?filter=pending" as any)}
            iconBg="#FFF7E6"
            iconColor={Colors[theme].accent}
            valueBg="#FEF3C7"
            valueColor="#D97706"
          />
          <SummaryCard
            theme={theme}
            icon="cloud-done"
            title="Synced"
            value={counts.synced}
            onPress={() => router.push("/local-registrations?filter=synced" as any)}
            iconBg="#FFF7E6"
            iconColor={Colors[theme].accent}
            valueBg="#E8F3EE"
            valueColor={Colors[theme].primary}
          />
          <SummaryCard
            theme={theme}
            icon="error-outline"
            title="Failed"
            value={counts.failed}
            onPress={() => router.push("/local-registrations?filter=failed" as any)}
            iconBg="#FFF7E6"
            iconColor={Colors[theme].accent}
            valueBg="#FEE2E2"
            valueColor="#B91C1C"
          />
        </View>

        <Pressable onPress={() => router.push("/local-registrations")} style={({ pressed }) => [styles.linkRow, pressed && { opacity: 0.85 }]}>
          <ThemedText style={styles.linkText}>View all saved registrations</ThemedText>
          <MaterialIcons name="chevron-right" size={20} color={Colors[theme].muted} />
        </Pressable>
      </View>

      <FlatList
        data={items}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <Pressable
            onPress={() => {
              Alert.alert(
                "Options",
                "What would you like to do?",
                [
                  {
                    text: "Edit / Resubmit",
                    onPress: async () => {
                      const draft = {
                        step: 1,
                        growerType: item.grower.growerType,
                        firstName: item.grower.firstName,
                        middleName: item.grower.middleName,
                        lastName: item.grower.lastName,
                        nrcNumber: item.grower.nrcNumber,
                        sex: item.grower.sex,
                        dateOfBirth: item.grower.dateOfBirth,
                        phoneNumber: item.grower.phoneNumber,
                        email: item.grower.email,
                        address: item.grower.address,
                        townOrVillage: item.grower.townOrVillage,
                        province: item.grower.province,
                        district: item.grower.district,
                        gpsLatitude: item.grower.gpsLatitude,
                        gpsLongitude: item.grower.gpsLongitude,
                        profilePhotoUri: item.grower.profilePhotoUri,
                        idFrontUri: item.grower.idFrontUri,
                        idBackUri: item.grower.idBackUri,
                        auditDateTime: new Date().toLocaleString("en-GB", {
                          day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit",
                        }),
                        editingId: item.id,
                        crop: item.crop,
                      };
                      await AsyncStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
                      await AsyncStorage.removeItem(CLEAR_FORM_KEY);
                      router.push("/registration/new");
                    }
                  },
                  {
                    text: "View Details",
                    onPress: () => {
                      Alert.alert(
                        "Saved Registration",
                        [
                          `Status: ${item.syncStatus}`,
                          item.lastError ? `Last error: ${item.lastError}` : "",
                          `Created: ${formatWhen(item.createdAt)}`,
                          "",
                          `Grower Type: ${item.grower.growerType}`,
                          `Name: ${fullName(item)}`,
                          `NRC/ID: ${item.grower.nrcNumber}`,
                          `Phone: ${item.grower.phoneNumber}`,
                          `Province: ${item.grower.province}`,
                          `District: ${item.grower.district}`,
                        ].filter(Boolean).join("\n"),
                      );
                    }
                  },
                  { text: "Cancel", style: "cancel" },
                ]
              );
            }}
            style={[
              styles.row,
              { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface },
            ]}
          >
            <View style={styles.rowTop}>
              <View style={[styles.rowIconBox, { backgroundColor: "#FFF7E6" }]}>
                <MaterialIcons name="person" size={18} color={Colors[theme].accent} />
              </View>
              <View style={styles.rowTextBlock}>
                <ThemedText type="defaultSemiBold" numberOfLines={1}>
                  {fullName(item) || "Unnamed grower"}
                </ThemedText>
                <ThemedText style={styles.rowMeta} numberOfLines={1}>
                  {item.grower.nrcNumber} • {item.grower.province} / {item.grower.district}
                </ThemedText>
              </View>
              <StatusPill status={item.syncStatus} />
            </View>
            <ThemedText style={styles.rowTime}>{formatWhen(item.createdAt)}</ThemedText>
          </Pressable>
        )}
        ListEmptyComponent={
          <View
            style={[
              styles.empty,
              { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface },
            ]}
          >
            <ThemedText type="defaultSemiBold">No saved registrations</ThemedText>
            <ThemedText>Tap “Register New Grower” to create one.</ThemedText>
          </View>
        }
      />
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#FFFFFF" },
  header: { paddingHorizontal: 20, paddingTop: 12, gap: 12, paddingBottom: 14 },
  topRow: { flexDirection: "row", alignItems: "center", justifyContent: "flex-end", gap: 12 },
  primaryPill: { backgroundColor: "#0B6B3A", paddingHorizontal: 14, paddingVertical: 10, borderRadius: 999, flexDirection: "row", alignItems: "center", gap: 8 },
  primaryPillText: { color: "#FFFFFF", fontSize: 13, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  summaryGrid: { flexDirection: "row", flexWrap: "wrap", justifyContent: "space-between", rowGap: 12, columnGap: 12, marginTop: 2 },
  summaryCard: { width: "48%", borderWidth: 1, borderRadius: 18, padding: 14, overflow: "hidden" },
  summaryCardTop: { flexDirection: "row", alignItems: "flex-start", justifyContent: "space-between", gap: 10 },
  summaryIconBox: { width: 42, height: 42, borderRadius: 14, alignItems: "center", justifyContent: "center" },
  summaryTitle: { flex: 1, fontSize: 15, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#152036" },
  summarySub: { marginTop: 6, fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, color: "#6B7280" },
  summaryCardBottom: { marginTop: 10, flexDirection: "row", alignItems: "center", justifyContent: "space-between", gap: 10 },
  summaryValuePill: { minWidth: 44, height: 30, borderRadius: 999, paddingHorizontal: 10, alignItems: "center", justifyContent: "center" },
  summaryValue: { fontSize: 13, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  linkRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingVertical: 10 },
  linkText: { fontSize: 13, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, color: "#6B7280" },
  list: { paddingHorizontal: 20, paddingBottom: 20, gap: 12 },
  row: { borderWidth: 1, borderRadius: 18, padding: 14, gap: 10, overflow: "hidden" },
  rowTop: { flexDirection: "row", alignItems: "center", gap: 12 },
  rowIconBox: { width: 42, height: 42, borderRadius: 14, alignItems: "center", justifyContent: "center" },
  rowTextBlock: { flex: 1, gap: 2 },
  rowMeta: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, color: "#6B7280" },
  rowTime: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, color: "#6B7280" },
  statusPill: { paddingHorizontal: 10, paddingVertical: 6, borderRadius: 999, overflow: "hidden" },
  statusText: { fontSize: 11, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  empty: { borderWidth: 1, borderRadius: 18, padding: 14, gap: 6, overflow: "hidden" },
});
