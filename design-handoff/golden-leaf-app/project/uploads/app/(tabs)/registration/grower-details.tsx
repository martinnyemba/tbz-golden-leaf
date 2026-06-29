import React, { useCallback, useState, useEffect, useRef } from "react";
import { View, ScrollView, StyleSheet, Linking, Pressable } from "react-native";
import { useFocusEffect, useLocalSearchParams, useRouter } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { FullScreenLeafLoader } from "@/components/LeafLoader";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiErrorUi, apiFetchJson } from "@/lib/inspection-storage";
import { readCacheItems } from "@/lib/offline-cache";
import AsyncStorage from "@react-native-async-storage/async-storage";

const API_BASE_URL_KEY = "tbz:portalBaseUrl:v1";

function formatWhen(iso: string) {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString([], { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function formatDateOnly(iso: string) {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' });
}

function fullName(g: any) {
  if (!g) return "";
  return [g.first_name, g.middle_name, g.last_name].filter(Boolean).join(" ").trim();
}

function badgeColor(status: string) {
  if (status === "ACTIVE" || status === "APPROVED") return "#10B981"; // success
  if (status === "PENDING" || status === "DRAFT") return "#F59E0B"; // warning
  if (status === "REJECTED" || status === "SUSPENDED") return "#EF4444"; // danger
  return "#6B7280"; // default muted
}

export default function GrowerDetailsScreen() {
  const { id } = useLocalSearchParams();
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  
  const [grower, setGrower] = useState<any>(null);
  const [crops, setCrops] = useState<any[]>([]);
  const [stopOrders, setStopOrders] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [baseUrl, setBaseUrl] = useState("");
  const [loadError, setLoadError] = useState("");

  useFocusEffect(
    useCallback(() => {
      let isMounted = true;
      (async () => {
        const savedBase = (await AsyncStorage.getItem(API_BASE_URL_KEY)) ?? "";
        if (savedBase && isMounted) setBaseUrl(savedBase.replace(/\/+$/, ""));
        
        if (!id) return;
        try {
          setLoadError("");
          const [growerResp, cropsResp, stopOrdersResp] = await Promise.all([
            apiFetchJson(`/api/v1/growers/growers/${id}/`, { method: "GET" }),
            apiFetchJson(`/api/v1/growers/crop-allocations/?grower_season__grower=${id}`, { method: "GET" }),
            apiFetchJson(`/api/v1/finance/stop-orders/?grower=${id}`, { method: "GET" })
          ]);

          if (!isMounted) return;

          const safeJson = (raw: string) => {
            try {
              return JSON.parse(raw) as any;
            } catch {
              return null;
            }
          };

          let growerData: any = null;
          if (growerResp.ok) growerData = safeJson(growerResp.body);

          if (!growerData) {
            const cached = await readCacheItems<any>("growers");
            const wanted = decodeURIComponent(String(id)).trim().toLowerCase().replace(/\s+/g, "");
            const found =
              cached.find((g) => String(g?.id ?? "") === String(id)) ??
              cached.find((g) => String(g?.id ?? "").trim().toLowerCase() === wanted) ??
              cached.find((g) => String(g?.tbz_id ?? "").trim().toLowerCase() === wanted) ??
              cached.find((g) => String(g?.nrc_number ?? "").trim().toLowerCase().replace(/\s+/g, "") === wanted) ??
              null;
            if (found) growerData = found;
          }

          if (growerData) setGrower(growerData);
          if (!growerData && !growerResp.ok) {
            const ui = apiErrorUi(growerResp.status, growerResp.body);
            setLoadError(ui.message);
          }
          if (cropsResp.ok) {
            const cBody = safeJson(cropsResp.body);
            if (cBody) setCrops(cBody.results || (Array.isArray(cBody) ? cBody : []));
          }
          if (stopOrdersResp.ok) {
            const sBody = safeJson(stopOrdersResp.body);
            if (sBody) setStopOrders(sBody.results || (Array.isArray(sBody) ? sBody : []));
          }
        } catch (e) {
          const msg = e instanceof Error ? e.message : "Network request failed.";
          setLoadError(msg);
        } finally {
          if (isMounted) setLoading(false);
        }
      })();
      return () => {
        isMounted = false;
      };
    }, [id])
  );

  if (loading) {
    return <FullScreenLeafLoader label="Loading grower profile..." />;
  }

  if (!grower) {
    return (
      <ThemedView style={styles.centerMode}>
        <MaterialIcons name="error-outline" size={48} color={Colors[theme].muted} />
        <ThemedText style={{ marginTop: 12, color: Colors[theme].muted, textAlign: "center" }}>
          {loadError || "Could not load grower details."}
        </ThemedText>
      </ThemedView>
    );
  }

  const openDoc = (path: string) => {
    if (!path) return;
    const url = path.startsWith("http") ? path : `${baseUrl}${path.startsWith("/") ? path : `/${path}`}`;
    Linking.openURL(url).catch(() => {});
  };

  const cropEntriesCount = crops.length;
  const seasonsCount = new Set(crops.map((c: any) => c.season_year).filter(Boolean)).size;
  const totalHectarage = crops.reduce((sum, c) => sum + (parseFloat(c.hectarage) || 0), 0);
  const totalYield = crops.reduce((sum, c) => sum + (parseFloat(c.expected_yield_kg) || 0), 0);

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        
        {/* Header Strip */}
        <View style={[styles.card, { backgroundColor: Colors[theme].surface }]}>
          <View style={styles.headerRow}>
            <View style={styles.avatar}>
              <MaterialIcons name="person" size={32} color="#FFFFFF" />
            </View>
            <View style={styles.headerInfo}>
              <ThemedText type="subtitle" numberOfLines={1} style={styles.headerName}>
                {grower.display_name || fullName(grower) || "Unnamed"}
              </ThemedText>
              <View style={styles.badgeRow}>
                {grower.tbz_id && (
                  <View style={[styles.badge, { backgroundColor: "#D1FAE5" }]}>
                    <ThemedText style={styles.badgeTextGreen}>{grower.tbz_id}</ThemedText>
                  </View>
                )}
                <View style={[styles.badge, { backgroundColor: badgeColor(grower.status) }]}>
                  <ThemedText style={styles.badgeTextWhite}>{grower.status_display || grower.status}</ThemedText>
                </View>
              </View>
              <View style={styles.registeredRow}>
                <MaterialIcons name="calendar-today" size={12} color={Colors[theme].muted} />
                <ThemedText style={[styles.registeredText, { color: Colors[theme].muted }]}>
                  Registered {formatDateOnly(grower.created_at)}
                </ThemedText>
              </View>
            </View>
            <Pressable
              style={({ pressed }) => [styles.editButton, { opacity: pressed ? 0.8 : 1 }]}
              onPress={() => router.push({ pathname: "/registration/grower-edit", params: { id: grower.id } } as any)}
            >
              <MaterialIcons name="edit" size={14} color="#4B5563" />
              <ThemedText style={styles.editButtonText}>Edit</ThemedText>
            </Pressable>
          </View>
        </View>

        {/* Personal Info */}
        <View style={[styles.card, { backgroundColor: Colors[theme].surface }]}>
            <View style={styles.cardSectionHeader}>
              <MaterialIcons name="person" size={20} color={Colors[theme].primary} style={{ marginRight: 8 }} />
              <ThemedText type="defaultSemiBold">Personal Information</ThemedText>
            </View>
            <View style={styles.dataGrid}>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Full Name</ThemedText>
                <ThemedText style={styles.dataValue}>{fullName(grower)}</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>NRC / PACRA</ThemedText>
                <ThemedText style={styles.dataValue}>{grower.nrc_number || "—"}</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Sex</ThemedText>
                <ThemedText style={styles.dataValue}>{grower.sex || "—"}</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Date of Birth</ThemedText>
                <ThemedText style={styles.dataValue}>{formatDateOnly(grower.date_of_birth)}</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Phone Number</ThemedText>
                <ThemedText style={styles.dataValue}>{grower.phone_number || "—"}</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Email</ThemedText>
                <ThemedText style={styles.dataValue}>{grower.email || "—"}</ThemedText>
              </View>
            </View>
        </View>

        {/* Location Info */}
        <View style={[styles.card, { backgroundColor: Colors[theme].surface }]}>
            <View style={styles.cardSectionHeader}>
              <MaterialIcons name="place" size={20} color={Colors[theme].primary} style={{ marginRight: 8 }} />
              <ThemedText type="defaultSemiBold">Location</ThemedText>
            </View>
            <View style={styles.dataGrid}>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Address / Farm</ThemedText>
                <ThemedText style={styles.dataValue}>{grower.address || "—"}</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Town / Village</ThemedText>
                <ThemedText style={styles.dataValue}>{grower.town_or_village || "—"}</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Province</ThemedText>
                <ThemedText style={styles.dataValue}>{grower.province_display || grower.province || "—"}</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>District</ThemedText>
                <ThemedText style={styles.dataValue}>{grower.district || "—"}</ThemedText>
              </View>
              {grower.gps_latitude && grower.gps_longitude && (
                <View style={[styles.dataCell, { width: '100%' }]}>
                  <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>GPS Coordinates</ThemedText>
                  <ThemedText style={styles.dataValue}>{grower.gps_latitude}, {grower.gps_longitude}</ThemedText>
                </View>
              )}
            </View>
        </View>

        {/* Crop Summary */}
        <View style={[styles.card, { backgroundColor: Colors[theme].surface }]}>
            <View style={[styles.cardSectionHeader, { justifyContent: "space-between" }]}>
              <View style={{ flexDirection: "row", alignItems: "center" }}>
                <MaterialIcons name="eco" size={20} color={Colors[theme].primary} style={{ marginRight: 8 }} />
                <ThemedText type="defaultSemiBold">Crop Info</ThemedText>
              </View>
              <Pressable 
                style={{ backgroundColor: "#0B6B3A", paddingHorizontal: 12, paddingVertical: 6, borderRadius: 16, flexDirection: "row", alignItems: "center", gap: 4 }}
                onPress={() => {
                  router.push({
                    pathname: "/registration/crop-allocation",
                    params: { growerId: grower.id }
                  } as any);
                }}
              >
                <MaterialIcons name="add" size={16} color="#FFFFFF" />
                <ThemedText style={{ color: "#FFFFFF", fontSize: 13, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>Add Crop</ThemedText>
              </Pressable>
            </View>
            <View style={styles.dataGrid}>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Crop Entries</ThemedText>
                <ThemedText style={styles.dataValue}>{cropEntriesCount}</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Seasons</ThemedText>
                <ThemedText style={styles.dataValue}>{seasonsCount}</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Total Hectarage</ThemedText>
                <ThemedText style={styles.dataValue}>{totalHectarage.toLocaleString()} ha</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Expected Yield</ThemedText>
                <ThemedText style={styles.dataValue}>{totalYield.toLocaleString()} kg</ThemedText>
              </View>
            </View>
        </View>

        {/* Detailed Crop Allocations */}
        {crops.map((crop, index) => (
          <View key={crop.id || index.toString()} style={[styles.card, { backgroundColor: Colors[theme].surface, opacity: 0.95 }]}>
            <View style={[styles.cardSectionHeader, { borderBottomWidth: 1, borderBottomColor: Colors[theme].border, paddingBottom: 8, marginBottom: 12 }]}>
              <MaterialIcons name="grass" size={20} color={Colors[theme].primary} style={{ marginRight: 8 }} />
              <View style={{ flex: 1, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                <ThemedText type="defaultSemiBold" style={{ flex: 1 }}>Crop {index + 1} - {crop.season_year || "Unknown Season"}</ThemedText>
                <View style={{ flexDirection: "row", gap: 8, alignItems: "center" }}>
                  <Pressable 
                    style={{ backgroundColor: "#F3F4F6", borderColor: "#D1D5DB", borderWidth: 1, paddingHorizontal: 10, paddingVertical: 4, borderRadius: 12, flexDirection: "row", alignItems: "center", gap: 4 }}
                    onPress={() => {
                      router.push({
                        pathname: "/registration/crop-allocation",
                        params: { growerId: grower.id, cropId: crop.id }
                      } as any);
                    }}
                  >
                    <MaterialIcons name="edit" size={12} color="#4B5563" />
                    <ThemedText style={{ color: "#4B5563", fontSize: 11, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>Update</ThemedText>
                  </Pressable>
                </View>
              </View>
            </View>
            <View style={styles.dataGrid}>
              <View style={[styles.dataCell, { width: '100%' }]}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Sponsorship</ThemedText>
                {crop.is_self_sponsored ? (
                  <View style={[styles.badge, { backgroundColor: "#E0E7FF", alignSelf: "flex-start", marginTop: 4 }]}>
                    <ThemedText style={{ fontSize: 11, color: "#4338CA", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>Self Sponsored</ThemedText>
                  </View>
                ) : crop.sponsor_name ? (
                  <ThemedText style={styles.dataValue}>{crop.sponsor_name}</ThemedText>
                ) : (
                  <View style={[styles.badge, { backgroundColor: "#FEF3C7", alignSelf: "flex-start", marginTop: 4 }]}>
                    <ThemedText style={{ fontSize: 11, color: "#B45309", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>Sponsored</ThemedText>
                  </View>
                )}
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Tobacco Type</ThemedText>
                <ThemedText style={styles.dataValue}>{crop.tobacco_type ? crop.tobacco_type.replace(/_/g, " ") : "—"}</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Hectarage</ThemedText>
                <ThemedText style={styles.dataValue}>{crop.hectarage ? parseFloat(crop.hectarage).toLocaleString() : "0"} ha</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Yield per Ha.</ThemedText>
                <ThemedText style={styles.dataValue}>{crop.yield_per_ha ? parseFloat(crop.yield_per_ha).toLocaleString() : "—"} kg</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Total Expected</ThemedText>
                <ThemedText style={styles.dataValue}>{crop.expected_yield_kg ? parseFloat(crop.expected_yield_kg).toLocaleString() : "—"} kg</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>Barn Type</ThemedText>
                <ThemedText style={styles.dataValue}>{crop.barn_type ? crop.barn_type.replace(/_/g, " ") : "—"}</ThemedText>
              </View>
              <View style={styles.dataCell}>
                <ThemedText style={[styles.dataLabel, { color: Colors[theme].muted }]}>No. of Barns</ThemedText>
                <ThemedText style={styles.dataValue}>{crop.number_of_barns || "—"}</ThemedText>
              </View>
            </View>
          </View>
        ))}

        {/* Documents */}
        {(grower.profile_photo || grower.id_front || grower.id_back) && (
          <View style={[styles.card, { backgroundColor: Colors[theme].surface }]}>
            <View style={styles.cardSectionHeader}>
              <MaterialIcons name="file-present" size={20} color={Colors[theme].primary} style={{ marginRight: 8 }} />
              <ThemedText type="defaultSemiBold">Documents</ThemedText>
            </View>
            <View style={{ gap: 8 }}>
              {grower.profile_photo && (
                <Pressable style={[styles.docLink, { borderColor: Colors[theme].border }]} onPress={() => openDoc(grower.profile_photo)}>
                  <MaterialIcons name="person" size={18} color={Colors[theme].text} style={{ marginRight: 8 }} />
                  <ThemedText style={{ flex: 1 }}>Profile Photo</ThemedText>
                  <MaterialIcons name="open-in-new" size={16} color={Colors[theme].muted} />
                </Pressable>
              )}
              {grower.id_front && (
                <Pressable style={[styles.docLink, { borderColor: Colors[theme].border }]} onPress={() => openDoc(grower.id_front)}>
                  <MaterialIcons name="badge" size={18} color={Colors[theme].text} style={{ marginRight: 8 }} />
                  <ThemedText style={{ flex: 1 }}>ID Front</ThemedText>
                  <MaterialIcons name="open-in-new" size={16} color={Colors[theme].muted} />
                </Pressable>
              )}
              {grower.id_back && (
                <Pressable style={[styles.docLink, { borderColor: Colors[theme].border }]} onPress={() => openDoc(grower.id_back)}>
                  <MaterialIcons name="badge" size={18} color={Colors[theme].text} style={{ marginRight: 8 }} />
                  <ThemedText style={{ flex: 1 }}>ID Back</ThemedText>
                  <MaterialIcons name="open-in-new" size={16} color={Colors[theme].muted} />
                </Pressable>
              )}
            </View>
          </View>
        )}

        {/* Audit Trail */}
        <View style={[styles.card, { backgroundColor: Colors[theme].surface }]}>
          <View style={styles.cardSectionHeader}>
            <MaterialIcons name="shield" size={20} color={Colors[theme].primary} style={{ marginRight: 8 }} />
            <ThemedText type="defaultSemiBold">Audit Trail</ThemedText>
          </View>
          <View style={styles.dataGrid}>
            <View style={[styles.dataCell, { width: '100%', flexDirection: 'row', justifyContent: 'space-between' }]}>
              <ThemedText style={{ color: Colors[theme].muted }}>Submitted by</ThemedText>
              <ThemedText style={{ fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>{grower.submitted_by_name || grower.submitted_by?.full_name || grower.submitted_by || "—"}</ThemedText>
            </View>
            <View style={[styles.dataCell, { width: '100%', flexDirection: 'row', justifyContent: 'space-between' }]}>
              <ThemedText style={{ color: Colors[theme].muted }}>Submitted on</ThemedText>
              <ThemedText style={{ fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>{formatWhen(grower.created_at)}</ThemedText>
            </View>
            {grower.approved_by && (
              <View style={[styles.dataCell, { width: '100%', flexDirection: 'row', justifyContent: 'space-between' }]}>
                <ThemedText style={{ color: Colors[theme].muted }}>Reviewed by</ThemedText>
                <ThemedText style={{ fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>{grower.approved_by_name || grower.approved_by?.full_name || grower.approved_by || "—"}</ThemedText>
              </View>
            )}
            {grower.approved_by && (
              <View style={[styles.dataCell, { width: '100%', flexDirection: 'row', justifyContent: 'space-between' }]}>
                <ThemedText style={{ color: Colors[theme].muted }}>Reviewed on</ThemedText>
                <ThemedText style={{ fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>{formatWhen(grower.approval_date)}</ThemedText>
              </View>
            )}
            {grower.rejection_reason && (
              <View style={[styles.dataCell, { width: '100%', flexDirection: 'column', gap: 4, marginTop: 4 }]}>
                <ThemedText style={{ color: Colors[theme].muted }}>Rejection Reason</ThemedText>
                <ThemedText style={{ color: "#EF4444", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>{grower.rejection_reason}</ThemedText>
              </View>
            )}
          </View>
        </View>

        {/* Stop Orders */}
        {stopOrders.length > 0 && (
          <View style={[styles.card, { backgroundColor: Colors[theme].surface }]}>
            <View style={[styles.cardSectionHeader, { justifyContent: "space-between" }]}>
              <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                <MaterialIcons name="lock" size={20} color="#F59E0B" style={{ marginRight: 8 }} />
                <ThemedText type="defaultSemiBold">Stop Orders</ThemedText>
              </View>
              <View style={[styles.badge, { backgroundColor: "#FEF3C7" }]}>
                <ThemedText style={{ fontSize: 12, color: "#92400E", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>{stopOrders.length}</ThemedText>
              </View>
            </View>
            <View style={{ gap: 12 }}>
              {stopOrders.map((so: any) => (
                <View key={so.id} style={{ borderBottomWidth: 1, borderBottomColor: Colors[theme].border, paddingBottom: 10 }}>
                  <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                    <ThemedText style={{ fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>{so.beneficiary_name}</ThemedText>
                    <View style={[styles.badge, { backgroundColor: badgeColor(so.status), paddingHorizontal: 6, paddingVertical: 2 }]}>
                      <ThemedText style={{ fontSize: 10, color: "#FFFFFF", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>{so.status_display || so.status}</ThemedText>
                    </View>
                  </View>
                  <ThemedText style={{ fontSize: 13, color: Colors[theme].muted }}>
                    ZMW {parseFloat(so.amount || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} — {so.beneficiary_type_display || so.beneficiary_type}
                  </ThemedText>
                </View>
              ))}
            </View>
          </View>
        )}

      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  scrollContent: { padding: 16, paddingBottom: 40, gap: 16 },
  centerMode: { flex: 1, alignItems: "center", justifyContent: "center" },
  card: {
    borderRadius: 16,
    padding: 16,
    overflow: "hidden",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.04,
    shadowRadius: 8,
    elevation: 2,
  },
  headerRow: { flexDirection: "row", gap: 14, alignItems: "flex-start" },
  avatar: { width: 56, height: 56, borderRadius: 14, backgroundColor: "#0B6B3A", alignItems: "center", justifyContent: "center" },
  headerInfo: { flex: 1, minWidth: 0, paddingTop: 1 },
  headerName: { fontSize: 18, lineHeight: 22 },
  registeredRow: { flexDirection: "row", alignItems: "center", gap: 6, marginTop: 6 },
  registeredText: { fontSize: 13, lineHeight: 18 },
  badgeRow: { flexDirection: "row", gap: 8, marginTop: 8, flexWrap: "wrap" },
  badge: { paddingHorizontal: 8, paddingVertical: 4, borderRadius: 8 },
  badgeTextGreen: { fontSize: 12, color: "#0B6B3A", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  badgeTextWhite: { fontSize: 12, color: "#FFFFFF", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  editButton: {
    backgroundColor: "#F3F4F6",
    borderColor: "#D1D5DB",
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 16,
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    alignSelf: "flex-start",
    marginTop: 2,
  },
  editButtonText: { color: "#4B5563", fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  cardSectionHeader: { flexDirection: "row", alignItems: "center", marginBottom: 12 },
  dataGrid: { flexDirection: "row", flexWrap: "wrap", gap: 12 },
  dataCell: { width: '47%', marginBottom: 4 },
  dataLabel: { fontSize: 12, marginBottom: 2, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular },
  dataValue: { fontSize: 14, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  docLink: { flexDirection: "row", alignItems: "center", padding: 10, borderWidth: 1, borderRadius: 8, overflow: "hidden" }
});
