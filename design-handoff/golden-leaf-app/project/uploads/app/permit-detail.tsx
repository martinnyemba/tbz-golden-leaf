import { useCallback, useMemo, useState } from "react";
import { ScrollView, StyleSheet, View, Alert, Modal, Pressable, Platform, TextInput } from "react-native";
import { useFocusEffect, useLocalSearchParams, useRouter, Stack } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton } from "@/components/ui/form-controls";
import { LeafLoader } from "@/components/LeafLoader";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiErrorUi, apiFetchJson, extractErrorMessage } from "@/lib/inspection-storage";
import QRCode from 'react-native-qrcode-svg';
import * as FileSystem from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';
import AsyncStorage from "@react-native-async-storage/async-storage";
import DateTimePicker from "@react-native-community/datetimepicker";

const API_BASE_URL_KEY = "tbz:portalBaseUrl:v1";
const AUTH_ACCESS_KEY = "tbz:portalAccessToken:v1";

function normalizeBaseUrl(raw: string) {
  return raw.trim().replace(/\/+$/, "");
}

function runtimePortalBaseUrl(raw: string) {
  const value = normalizeBaseUrl(raw);
  if (!value) return "";
  try {
    const url = new URL(value);
    const host = url.hostname;
    // Map localhost to Android emulator bridge
    if (Platform.OS === "android" && (host === "localhost" || host === "127.0.0.1")) {
      url.hostname = "10.0.2.2";
      return normalizeBaseUrl(url.toString());
    }
    return normalizeBaseUrl(url.toString());
  } catch {
    return value;
  }
}

export default function PermitDetailScreen() {
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  
  const [permit, setPermit] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [qrVisible, setQrVisible] = useState(false);
  const [actionsVisible, setActionsVisible] = useState(false);
  const [reviewAction, setReviewAction] = useState<"approve" | "reject">("approve");
  const [validFrom, setValidFrom] = useState<Date>(() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    d.setHours(0, 0, 0, 0);
    return d;
  });
  const [validTo, setValidTo] = useState<Date>(() => {
    const d = new Date();
    d.setDate(d.getDate() + 2);
    d.setHours(0, 0, 0, 0);
    return d;
  });
  const [rejectReason, setRejectReason] = useState("");
  const [showValidFromPicker, setShowValidFromPicker] = useState(false);
  const [showValidToPicker, setShowValidToPicker] = useState(false);

  const fetchPermitDetails = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const resp = await apiFetchJson(`/api/v1/permits/transport-permits/${id}/`, { method: "GET" });
      if (resp.ok) {
        const data = JSON.parse(resp.body);
        setPermit(data);
      } else {
        const ui = apiErrorUi(resp.status, resp.body);
        if (ui.kind === "login_required") {
          Alert.alert(ui.title, ui.message, [
            { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: `/permit-detail?id=${encodeURIComponent(String(id))}` } } as any) },
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
  }, [id]);

  useFocusEffect(
    useCallback(() => {
      fetchPermitDetails();
    }, [fetchPermitDetails])
  );

  const downloadPermitPdf = async () => {
    try {
      setLoading(true);
      const baseUrlFull = await AsyncStorage.getItem(API_BASE_URL_KEY);
      const token = await AsyncStorage.getItem(AUTH_ACCESS_KEY);

      if (!baseUrlFull || !token) {
        Alert.alert("Authentication Error", "You must be logged in to download permits.");
        return;
      }

      const baseUrl = runtimePortalBaseUrl(baseUrlFull);
      const url = `${baseUrl}/api/v1/permits/transport-permits/${id}/print/`;
      const safeName = String(permit?.permit_number || id).replace(/[^a-zA-Z0-9_-]/g, '_');
      const fileName = `Transport_Permit_${safeName}.pdf`;
      const fileUri = (FileSystem as any).documentDirectory + fileName;

      const downloadRes = await FileSystem.downloadAsync(url, fileUri, {
        headers: {
          "Authorization": `Bearer ${token}`
        }
      });

      if (downloadRes.status !== 200) {
        const hint =
          downloadRes.status === 401
            ? "Your portal session has expired. Please login again."
            : downloadRes.status === 404
              ? "PDF endpoint not found on the portal."
              : "";
        Alert.alert("Download failed", `Download failed (HTTP ${downloadRes.status}).${hint ? `\n\n${hint}` : ""}`);
        return;
      }

      const canShare = await Sharing.isAvailableAsync();
      if (canShare) {
        await Sharing.shareAsync(downloadRes.uri);
      } else {
        Alert.alert("Success", "File downloaded, but sharing is not available on this device.");
      }
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Download failed.";
      Alert.alert("Download failed", extractErrorMessage(msg) || msg);
    } finally {
      setLoading(false);
    }
  };

  const PENDING = permit?.status === "PENDING";
  const APPROVED = permit?.status === "APPROVED";
  
  const statusColor = APPROVED ? "#0B6B3A" : PENDING ? "#D97706" : "#DC2626";
  const statusBg = APPROVED ? "#E8F3EE" : PENDING ? "#FEF3C7" : "#FEE2E2";

  const calculateDaysValid = () => {
    if (!permit?.valid_from || !permit?.valid_to) return null;
    const end = new Date(permit.valid_to).getTime();
    const start = new Date(permit.valid_from).getTime();
    if (end < start) return null;
    return Math.floor((end - start) / (1000 * 60 * 60 * 24)) + 1;
  };
  const daysValid = calculateDaysValid();
  const isExpired = Boolean(permit?.valid_to) && new Date(permit.valid_to) < new Date();

  const canApproveReject = useMemo(() => {
    return Boolean(PENDING);
  }, [PENDING]);

  const canShowApprovedActions = useMemo(() => {
    return Boolean(APPROVED);
  }, [APPROVED]);

  const markAsUsed = useCallback(async () => {
    if (!id) return;
    Alert.alert("Mark as Used", "Mark this permit as used? This cannot be undone.", [
      { text: "Cancel", style: "cancel" },
      {
        text: "Mark Used",
        style: "destructive",
        onPress: async () => {
          setLoading(true);
          try {
            const resp = await apiFetchJson(`/api/v1/permits/transport-permits/${id}/mark-used/`, { method: "POST" });
            if (!resp.ok) {
              const ui = apiErrorUi(resp.status, resp.body);
              if (ui.kind === "login_required") {
                Alert.alert(ui.title, ui.message, [
                  { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: `/permit-detail?id=${encodeURIComponent(String(id))}` } } as any) },
                  { text: "OK" },
                ]);
              } else {
                Alert.alert(ui.title, ui.message);
              }
              return;
            }
            const data = JSON.parse(resp.body);
            setPermit(data);
            Alert.alert("Success", "Permit marked as used.");
          } catch (e) {
            const msg = e instanceof Error ? e.message : "Request failed.";
            Alert.alert("Request failed", msg);
          } finally {
            setLoading(false);
          }
        },
      },
    ]);
  }, [id]);

  const submitReview = useCallback(async () => {
    if (!id) return;
    if (reviewAction === "approve") {
      const from = new Date(validFrom);
      const to = new Date(validTo);
      from.setHours(0, 0, 0, 0);
      to.setHours(0, 0, 0, 0);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (from < today) {
        Alert.alert("Invalid dates", "Valid-from date cannot be in the past.");
        return;
      }
      if (to < from) {
        Alert.alert("Invalid dates", "Valid-to date cannot be earlier than valid-from date.");
        return;
      }
    } else {
      if (!rejectReason.trim()) {
        Alert.alert("Missing reason", "Rejection reason is required.");
        return;
      }
    }

    setLoading(true);
    try {
      const payload =
        reviewAction === "approve"
          ? {
              action: "approve",
              valid_from: validFrom.toISOString().slice(0, 10),
              valid_to: validTo.toISOString().slice(0, 10),
            }
          : { action: "reject", reason: rejectReason.trim() };

      const resp = await apiFetchJson(`/api/v1/permits/transport-permits/${id}/approve-reject/`, {
        method: "POST",
        body: JSON.stringify(payload),
      });
      if (!resp.ok) {
        const ui = apiErrorUi(resp.status, resp.body);
        if (ui.kind === "login_required") {
          Alert.alert(ui.title, ui.message, [
            { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: `/permit-detail?id=${encodeURIComponent(String(id))}` } } as any) },
            { text: "OK" },
          ]);
        } else {
          Alert.alert(ui.title, ui.message);
        }
        return;
      }
      const data = JSON.parse(resp.body);
      setPermit(data);
      setActionsVisible(false);
      Alert.alert("Success", reviewAction === "approve" ? "Permit approved." : "Permit rejected.");
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Request failed.";
      Alert.alert("Request failed", msg);
    } finally {
      setLoading(false);
    }
  }, [id, rejectReason, reviewAction, validFrom, validTo]);

  const formatDateShort = (dStr: string) => {
    if (!dStr) return "—";
    const d = new Date(dStr);
    return isNaN(d.getTime()) ? dStr : d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  };
  
  const formatDateLong = (dStr: string) => {
    if (!dStr) return "—";
    const d = new Date(dStr);
    return isNaN(d.getTime()) ? dStr : d.toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  };

  if (loading || !permit) {
    return (
      <ThemedView style={styles.container}>
        <Stack.Screen
          options={{
            headerShown: true,
            title: "Permit Details",
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
          title: permit.permit_number || "Permit Details",
          headerStyle: { backgroundColor: Colors[theme].primary },
          headerTitleStyle: { fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
          headerTintColor: Colors[theme].surface,
        }}
      />

      <ScrollView contentContainerStyle={styles.content}>
        
        {/* HERO CARD */}
        <View style={[styles.heroCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <View style={styles.heroTop}>
            <View style={styles.heroIconWrap}>
              <MaterialIcons name="local-shipping" size={28} color="#0B6B3A" />
            </View>
            <View style={{ flex: 1, marginLeft: 12 }}>
              <ThemedText style={[styles.heroTitle, { color: permit.permit_number ? "#0B6B3A" : "#6B7280" }]}>
                {permit.permit_number || "Permit Pending Approval"}
              </ThemedText>
              <View style={styles.heroBadges}>
                <View style={[styles.badge, { backgroundColor: statusBg }]}>
                  <ThemedText style={[styles.badgeText, { color: statusColor }]}>{permit.status}</ThemedText>
                </View>
                <View style={styles.badgeOutline}>
                  <ThemedText style={styles.badgeTextOutline}>{permit.purpose}</ThemedText>
                </View>
                <View style={styles.badgeOutline}>
                  <ThemedText style={styles.badgeTextOutline}>{permit.grower_category?.replace('_', ' ')}</ThemedText>
                </View>
              </View>
              <View style={[styles.badge, { alignSelf: 'flex-start', marginTop: 8, backgroundColor: permit.is_bought ? '#DCFCE7' : '#F3F4F6', borderWidth: 1, borderColor: permit.is_bought ? '#bbf7d0' : '#E5E7EB' }]}>
                 {permit.is_bought ? (
                   <ThemedText style={{ fontSize: 10, color: '#166534', fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }}>
                     <MaterialIcons name="check-circle" size={10} color="#166534" /> Bought
                   </ThemedText>
                 ) : (
                   <ThemedText style={{ fontSize: 10, color: '#4B5563', fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>
                     <MaterialIcons name="schedule" size={10} color="#4B5563" /> Not Yet Bought
                   </ThemedText>
                 )}
              </View>
            </View>
            {APPROVED && permit.qr_code_data && (
              <Pressable onPress={() => setQrVisible(true)} style={styles.qrButton}>
                <MaterialIcons name="qr-code-scanner" size={24} color="#0B6B3A" />
              </Pressable>
            )}
          </View>

          <View style={styles.heroBottomStrips}>
            <View style={styles.stripItem}>
              <MaterialIcons name="event" size={14} color="#6B7280" />
              <ThemedText style={styles.stripText}>Requested {formatDateShort(permit.date_requested)}</ThemedText>
            </View>
            <View style={styles.stripItem}>
              <MaterialIcons name="directions-car" size={14} color="#6B7280" />
              <ThemedText style={styles.stripText}>{permit.license_plate}</ThemedText>
            </View>
            <View style={{ width: '100%' }}>
              <View style={styles.stripItem}>
                <MaterialIcons name="location-on" size={14} color="#6B7280" />
                <ThemedText style={styles.stripText}>{permit.origin_province?.replace('_', ' ')}, {permit.origin_district} → {permit.destination_salesfloor}</ThemedText>
              </View>
            </View>
            {permit.valid_from && permit.valid_to && (
              <View style={styles.stripItem}>
                <MaterialIcons name="event-available" size={14} color="#6B7280" />
                <ThemedText style={styles.stripText}>Valid {formatDateShort(permit.valid_from)} – {formatDateShort(permit.valid_to)}</ThemedText>
              </View>
            )}
          </View>
        </View>

        {/* KPI ROW */}
        <View style={styles.kpiGrid}>
          <View style={[styles.kpiCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
             <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
               <ThemedText style={styles.kpiLabel}>Total Bales</ThemedText>
               <MaterialIcons name="inventory" size={14} color="#9CA3AF" />
             </View>
             <ThemedText style={styles.kpiVal}>{permit.total_bales}</ThemedText>
             <ThemedText style={styles.kpiSub}>bales authorised</ThemedText>
          </View>

          <View style={[styles.kpiCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
             <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
               <ThemedText style={styles.kpiLabel}>Total Weight</ThemedText>
               <MaterialIcons name="speed" size={14} color="#9CA3AF" />
             </View>
             <ThemedText style={styles.kpiVal}>{parseFloat(permit.total_weight_kg || 0).toFixed(1)}</ThemedText>
             <ThemedText style={styles.kpiSub}>kg authorised</ThemedText>
          </View>

          <View style={[styles.kpiCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
             <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
               <ThemedText style={styles.kpiLabel}>Validity</ThemedText>
               <MaterialIcons name="date-range" size={14} color="#9CA3AF" />
             </View>
             <ThemedText style={styles.kpiVal}>{daysValid ?? "—"}</ThemedText>
            <ThemedText style={[styles.kpiSub, isExpired && { color: '#DC2626', fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }]}>
               {!permit.valid_to ? "days (pending approval)" : isExpired ? "Expired" : "days validity"}
             </ThemedText>
          </View>

          <View style={[styles.kpiCard, { backgroundColor: APPROVED ? "#0B6B3A" : Colors[theme].surface, borderColor: Colors[theme].border }]}>
             <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
               <ThemedText style={[styles.kpiLabel, APPROVED && { color: 'rgba(255,255,255,0.8)' }]}>Status</ThemedText>
               <MaterialIcons name="verified" size={14} color={APPROVED ? "rgba(255,255,255,0.8)" : "#9CA3AF"} />
             </View>
             <ThemedText style={[styles.kpiVal, APPROVED && { color: '#FFF' }]}>{permit.status}</ThemedText>
             <ThemedText style={[styles.kpiSub, APPROVED && { color: 'rgba(255,255,255,0.8)' }]}>
               {permit.approved_by?.name || permit.approved_by_name ? `by ${permit.approved_by?.name || permit.approved_by_name}` : "awaiting review"}
             </ThemedText>
          </View>
        </View>

        {/* GROWER & TOBACCO DETAILS */}
        <View style={[styles.sectionCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <View style={styles.sectionHeader}>
            <MaterialIcons name="person" size={18} color="#0B6B3A" style={{ marginRight: 8 }} />
            <ThemedText style={styles.sectionTitle}>Grower & Tobacco Details</ThemedText>
          </View>
          <View style={styles.sectionBody}>
            <ThemedText style={styles.dividerLabel}>Grower</ThemedText>
            <View style={styles.row}>
              <ThemedText style={styles.rowLabel}>Full Name</ThemedText>
              <ThemedText style={[styles.rowVal, { color: '#0B6B3A', fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }]}>{permit.grower?.display_name || permit.grower_name}</ThemedText>
            </View>
            <View style={styles.row}>
              <ThemedText style={styles.rowLabel}>TBZ ID</ThemedText>
              <View style={styles.idBadge}>
                 <ThemedText style={styles.idBadgeText}>{permit.grower?.tbz_id || permit.grower_tbz_id || "—"}</ThemedText>
              </View>
            </View>
            <View style={styles.row}>
              <ThemedText style={styles.rowLabel}>Category</ThemedText>
              <ThemedText style={styles.rowVal}>{permit.grower_category?.replace('_', ' ')}</ThemedText>
            </View>

            <View style={[styles.dividerLine, { borderColor: Colors[theme].border }]} />
            <ThemedText style={styles.dividerLabel}>Tobacco</ThemedText>
            <View style={styles.row}>
              <ThemedText style={styles.rowLabel}>Purpose</ThemedText>
              <View style={[styles.badgeOutline, { paddingVertical: 1 }]}>
                  <ThemedText style={styles.badgeTextOutline}>{permit.purpose}</ThemedText>
              </View>
            </View>
            <View style={styles.row}>
              <ThemedText style={styles.rowLabel}>Bales Authorised</ThemedText>
              <ThemedText style={[styles.rowVal, { fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }]}>{permit.total_bales}</ThemedText>
            </View>
            <View style={styles.row}>
              <ThemedText style={styles.rowLabel}>Weight Authorised</ThemedText>
              <ThemedText style={[styles.rowVal, { fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }]}>{parseFloat(permit.total_weight_kg).toFixed(3)} kg</ThemedText>
            </View>
            <View style={styles.row}>
              <ThemedText style={styles.rowLabel}>Already Bought</ThemedText>
              {permit.is_bought ? (
                <View style={[styles.badge, { backgroundColor: '#166534', paddingVertical: 2, paddingHorizontal: 6, flexDirection: 'row', alignItems: 'center' }]}>
                  <MaterialIcons name="check" size={13} color="#FFF" style={{ marginRight: 2 }} />
                  <ThemedText style={{ color: '#FFF', fontSize: 11, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>Yes — Pre-sold</ThemedText>
                </View>
              ) : (
                <View style={[styles.badgeOutline, { paddingVertical: 2, paddingHorizontal: 6 }]}>
                   <ThemedText style={{ color: '#374151', fontSize: 11, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>No — Not yet bought</ThemedText>
                </View>
              )}
            </View>
          </View>
        </View>

        {/* TRANSPORT & ROUTE */}
        <View style={[styles.sectionCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <View style={styles.sectionHeader}>
            <MaterialIcons name="local-shipping" size={18} color="#0B6B3A" style={{ marginRight: 8 }} />
            <ThemedText style={styles.sectionTitle}>Transport & Route</ThemedText>
          </View>
          <View style={styles.sectionBody}>
            <View style={styles.row}>
              <ThemedText style={styles.rowLabel}>License Plate</ThemedText>
              <View style={{ backgroundColor: '#F3F4F6', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 4 }}>
                <ThemedText style={{ fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, fontSize: 13, color: '#111827' }}>{permit.license_plate}</ThemedText>
              </View>
            </View>
            <View style={styles.row}>
              <ThemedText style={styles.rowLabel}>Origin</ThemedText>
              <ThemedText style={styles.rowVal}>{permit.origin_province?.replace('_', ' ')} — {permit.origin_district}</ThemedText>
            </View>
            <View style={styles.row}>
              <ThemedText style={styles.rowLabel}>Destination</ThemedText>
              <ThemedText style={styles.rowVal}>{permit.destination_salesfloor}</ThemedText>
            </View>
            {permit.valid_from && (
              <>
                <View style={styles.row}>
                  <ThemedText style={styles.rowLabel}>Valid From</ThemedText>
                  <ThemedText style={styles.rowVal}>{formatDateShort(permit.valid_from)}</ThemedText>
                </View>
                <View style={[styles.row, { paddingBottom: 0 }]}>
                  <ThemedText style={styles.rowLabel}>Valid To</ThemedText>
                  <ThemedText style={[styles.rowVal, isExpired && { color: '#DC2626', fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }]}>
                    {formatDateShort(permit.valid_to)} 
                    {isExpired && (
                      <View style={[styles.badge, { backgroundColor: '#DC2626', marginLeft: 8, paddingVertical: 2 }]}>
                        <ThemedText style={{ color: '#FFF', fontSize: 10, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }}>Expired</ThemedText>
                      </View>
                    )}
                  </ThemedText>
                </View>
              </>
            )}
          </View>
        </View>

        {/* BUYER INFORMATION */}
        {permit.is_bought && (permit.buyer || permit.buyer_accepted) && (
          <View style={[styles.sectionCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
            <View style={styles.sectionHeader}>
              <MaterialIcons name="storefront" size={18} color="#0B6B3A" style={{ marginRight: 8 }} />
              <ThemedText style={styles.sectionTitle}>Buyer Information</ThemedText>
            </View>
            <View style={styles.sectionBody}>
              <View style={styles.row}>
                <ThemedText style={styles.rowLabel}>Assigned Buyer</ThemedText>
                <ThemedText style={[styles.rowVal, { fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }]}>{permit.buyer?.name || permit.buyer_name || "—"}</ThemedText>
              </View>
              <View style={[styles.row, { paddingBottom: 0 }]}>
                <ThemedText style={styles.rowLabel}>Buyer Accepted</ThemedText>
                {permit.buyer_accepted ? (
                  <View style={[styles.badge, { backgroundColor: '#166534', paddingVertical: 2, paddingHorizontal: 6, flexDirection: 'row', alignItems: 'center' }]}>
                    <MaterialIcons name="check-circle" size={12} color="#FFF" style={{ marginRight: 4 }} />
                    <ThemedText style={{ color: '#FFF', fontSize: 11, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>Confirmed — Accepted</ThemedText>
                  </View>
                ) : (
                  <View style={[styles.badge, { backgroundColor: '#FEF3C7', paddingVertical: 2, paddingHorizontal: 6, flexDirection: 'row', alignItems: 'center' }]}>
                    <MaterialIcons name="hourglass-empty" size={12} color="#D97706" style={{ marginRight: 4 }} />
                    <ThemedText style={{ color: '#D97706', fontSize: 11, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>Not Yet Confirmed</ThemedText>
                  </View>
                )}
              </View>
            </View>
          </View>
        )}

        {/* COMMENTS */}
        {permit.comments && (
          <View style={[styles.sectionCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
            <View style={styles.sectionHeader}>
              <MaterialIcons name="chat-bubble-outline" size={18} color="#0B6B3A" style={{ marginRight: 8 }} />
              <ThemedText style={styles.sectionTitle}>Comments</ThemedText>
            </View>
            <View style={[styles.sectionBody, { paddingTop: 12 }]}>
               <ThemedText style={{ fontSize: 13, color: '#4B5563', lineHeight: 20 }}>{permit.comments}</ThemedText>
            </View>
          </View>
        )}

        {/* REJECTION REASON */}
        {permit.rejection_reason && (
          <View style={{ backgroundColor: '#FEF2F2', borderColor: '#FECACA', borderWidth: 1, borderRadius: 12, padding: 16, flexDirection: 'row', alignItems: 'flex-start' }}>
            <MaterialIcons name="cancel" size={24} color="#DC2626" style={{ marginTop: 2, marginRight: 12 }} />
            <View style={{ flex: 1 }}>
              <ThemedText style={{ fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, color: '#991B1B', marginBottom: 4 }}>Permit Rejected</ThemedText>
              <ThemedText style={{ color: '#7F1D1D', fontSize: 13 }}>{permit.rejection_reason}</ThemedText>
            </View>
          </View>
        )}

        {/* AUDIT TRAIL */}
        <View style={[styles.sectionCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <View style={styles.sectionHeader}>
            <MaterialIcons name="verified-user" size={18} color="#0B6B3A" style={{ marginRight: 8 }} />
            <ThemedText style={styles.sectionTitle}>Audit Trail</ThemedText>
          </View>
          <View style={styles.sectionBody}>
            <View style={styles.auditRow}>
              <ThemedText style={styles.auditLabel}>Submitted by</ThemedText>
              <ThemedText style={styles.auditVal}>{permit.submitted_by?.name || permit.submitted_by_name || permit.submitted_by?.get_full_name || "—"}</ThemedText>
            </View>
            <View style={styles.auditRow}>
              <ThemedText style={styles.auditLabel}>Date requested</ThemedText>
              <ThemedText style={styles.auditVal}>{formatDateShort(permit.date_requested)}</ThemedText>
            </View>
            <View style={styles.auditRow}>
              <ThemedText style={styles.auditLabel}>Last updated</ThemedText>
              <ThemedText style={styles.auditVal}>{formatDateLong(permit.updated_at)}</ThemedText>
            </View>
            {permit.approved_by && (
              <View style={styles.auditRow}>
                <ThemedText style={styles.auditLabel}>Reviewed by</ThemedText>
                <ThemedText style={styles.auditVal}>{permit.approved_by_name || permit.approved_by?.name || permit.approved_by?.get_full_name || "—"}</ThemedText>
              </View>
            )}
            <View style={[styles.auditRow, { borderBottomWidth: 0, paddingBottom: 0 }]}>
              <ThemedText style={styles.auditLabel}>Status</ThemedText>
              <View style={[styles.badge, { backgroundColor: statusBg }]}>
                <ThemedText style={[styles.badgeText, { color: statusColor }]}>{permit.status}</ThemedText>
              </View>
            </View>
          </View>
        </View>
        
        <View style={[styles.sectionCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <View style={styles.sectionHeader}>
            <MaterialIcons name="bolt" size={18} color="#0B6B3A" style={{ marginRight: 8 }} />
            <ThemedText style={styles.sectionTitle}>Actions</ThemedText>
          </View>
          <View style={styles.sectionBody}>
            {canApproveReject && (
              <View style={{ marginBottom: 10 }}>
                <PrimaryButton title="Review & Approve" onPress={() => setActionsVisible(true)} />
              </View>
            )}

            {canShowApprovedActions && (
              <View style={{ gap: 10, marginBottom: 10 }}>
                <PrimaryButton title="Print / Download PDF" onPress={downloadPermitPdf} />
                <PrimaryButton title="Mark as Used" onPress={markAsUsed} />
              </View>
            )}

            <PrimaryButton title="Back to Permits" onPress={() => router.push("/permit-list" as any)} />
          </View>
        </View>

      </ScrollView>

      {/* QR MODAL */}
      <Modal visible={qrVisible} transparent animationType="fade" onRequestClose={() => setQrVisible(false)}>
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: Colors[theme].background }]}>
            <View style={{ width: '100%', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                <MaterialIcons name="qr-code" size={20} color="#0B6B3A" style={{ marginRight: 8 }} />
                <ThemedText type="subtitle">Permit QR Code</ThemedText>
              </View>
              <Pressable onPress={() => setQrVisible(false)}>
                <MaterialIcons name="close" size={24} color={Colors[theme].text} />
              </Pressable>
            </View>
            <View style={{ padding: 20, backgroundColor: '#FFFFFF', borderRadius: 12, elevation: 4 }}>
               <QRCode value={permit.qr_code_data || "INVALID"} size={200} backgroundColor="#FFFFFF" color="#000000" />
            </View>
            <ThemedText style={{ marginTop: 24, textAlign: 'center', fontSize: 13, color: '#6B7280' }}>Scan at salesfloor entry to verify permit validity.</ThemedText>
            <ThemedText style={{ marginTop: 8, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, fontSize: 11, color: '#9CA3AF' }}>{permit.permit_number}</ThemedText>
          </View>
        </View>
      </Modal>

      <Modal visible={actionsVisible} transparent animationType="fade" onRequestClose={() => setActionsVisible(false)}>
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: Colors[theme].background, alignItems: "stretch" }]}>
            <View style={{ width: "100%", flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
              <View style={{ flexDirection: "row", alignItems: "center" }}>
                <MaterialIcons name="checklist" size={20} color="#0B6B3A" style={{ marginRight: 8 }} />
                <ThemedText type="subtitle">Review & Approve</ThemedText>
              </View>
              <Pressable onPress={() => setActionsVisible(false)}>
                <MaterialIcons name="close" size={24} color={Colors[theme].text} />
              </Pressable>
            </View>

            <View style={{ flexDirection: "row", gap: 10, marginBottom: 12 }}>
              <Pressable
                onPress={() => setReviewAction("approve")}
                style={[
                  styles.pillButton,
                  { borderColor: Colors[theme].border, backgroundColor: reviewAction === "approve" ? "#0B6B3A" : Colors[theme].surface },
                ]}
              >
                <ThemedText style={{ color: reviewAction === "approve" ? "#fff" : Colors[theme].text, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>
                  Approve
                </ThemedText>
              </Pressable>
              <Pressable
                onPress={() => setReviewAction("reject")}
                style={[
                  styles.pillButton,
                  { borderColor: Colors[theme].border, backgroundColor: reviewAction === "reject" ? "#DC2626" : Colors[theme].surface },
                ]}
              >
                <ThemedText style={{ color: reviewAction === "reject" ? "#fff" : Colors[theme].text, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium }}>
                  Reject
                </ThemedText>
              </Pressable>
            </View>

            {reviewAction === "approve" ? (
              <View style={{ gap: 12 }}>
                <View>
                  <ThemedText style={styles.modalLabel}>Valid From</ThemedText>
                  <Pressable onPress={() => setShowValidFromPicker(true)} style={[styles.modalPicker, { borderColor: Colors[theme].border }]}>
                    <ThemedText>{formatDateShort(validFrom.toISOString())}</ThemedText>
                    <MaterialIcons name="event" size={18} color={Colors[theme].muted} />
                  </Pressable>
                  {showValidFromPicker && (
                    <DateTimePicker
                      value={validFrom}
                      mode="date"
                      display={Platform.OS === "ios" ? "spinner" : "default"}
                      onChange={(_e: any, selected?: Date) => {
                        setShowValidFromPicker(false);
                        if (selected) setValidFrom(selected);
                      }}
                    />
                  )}
                </View>

                <View>
                  <ThemedText style={styles.modalLabel}>Valid To</ThemedText>
                  <Pressable onPress={() => setShowValidToPicker(true)} style={[styles.modalPicker, { borderColor: Colors[theme].border }]}>
                    <ThemedText>{formatDateShort(validTo.toISOString())}</ThemedText>
                    <MaterialIcons name="event" size={18} color={Colors[theme].muted} />
                  </Pressable>
                  {showValidToPicker && (
                    <DateTimePicker
                      value={validTo}
                      mode="date"
                      display={Platform.OS === "ios" ? "spinner" : "default"}
                      onChange={(_e: any, selected?: Date) => {
                        setShowValidToPicker(false);
                        if (selected) setValidTo(selected);
                      }}
                    />
                  )}
                </View>
              </View>
            ) : (
              <View style={{ gap: 8 }}>
                <ThemedText style={styles.modalLabel}>Rejection Reason</ThemedText>
                <View style={[styles.modalInputWrap, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
                  <TextInput
                    value={rejectReason}
                    onChangeText={setRejectReason}
                    placeholder="Enter rejection reason..."
                    placeholderTextColor={Colors[theme].muted}
                    style={{ color: Colors[theme].text, minHeight: 70 }}
                    multiline
                  />
                </View>
              </View>
            )}

            <View style={{ marginTop: 16, gap: 10 }}>
              <PrimaryButton title={reviewAction === "approve" ? "Approve Permit" : "Reject Permit"} onPress={submitReview} />
              <PrimaryButton title="Cancel" onPress={() => setActionsVisible(false)} />
            </View>
          </View>
        </View>
      </Modal>

    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  headerRow: { flexDirection: "row", alignItems: "center", paddingHorizontal: 16, paddingTop: 16, paddingBottom: 8 },
  content: { padding: 16, paddingBottom: 40, gap: 16 },
  
  heroCard: {
    borderWidth: 1, borderRadius: 12, padding: 16, marginBottom: 4
  },
  heroTop: { flexDirection: 'row', alignItems: 'flex-start' },
  heroIconWrap: { width: 44, height: 44, borderRadius: 10, backgroundColor: '#E8F3EE', justifyContent: 'center', alignItems: 'center' },
  heroTitle: { fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, fontSize: 17, marginBottom: 8 },
  heroBadges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  badge: { paddingHorizontal: 8, paddingVertical: 4, borderRadius: 16 },
  badgeOutline: { paddingHorizontal: 8, paddingVertical: 4, borderRadius: 16, borderWidth: 1, borderColor: '#E5E7EB', backgroundColor: '#F9FAFB' },
  badgeText: { fontSize: 11, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  badgeTextOutline: { fontSize: 11, color: '#374151', fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  qrButton: { padding: 6, backgroundColor: '#E8F3EE', borderRadius: 8 },
  heroBottomStrips: { marginTop: 16, paddingTop: 16, borderTopWidth: 1, borderTopColor: '#F3F4F6', flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  stripItem: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  stripText: { fontSize: 12, color: '#6B7280' },

  kpiGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  kpiCard: { flexBasis: '47%', flexGrow: 1, borderWidth: 1, borderRadius: 12, padding: 14, justifyContent: 'center' },
  kpiLabel: { fontSize: 11, color: '#6B7280', marginBottom: 8, textTransform: 'uppercase', letterSpacing: 0.5, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  kpiVal: { fontSize: 24, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  kpiSub: { fontSize: 11, color: '#9CA3AF', marginTop: 4 },

  sectionCard: { borderWidth: 1, borderRadius: 12, overflow: 'hidden' },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 12, backgroundColor: '#F9FAFB', borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
  sectionTitle: { fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, fontSize: 14, color: '#111827' },
  sectionBody: { padding: 16 },
  
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 8 },
  rowLabel: { fontSize: 13, color: '#6B7280', flex: 1 },
  rowVal: { fontSize: 13, color: '#111827', flex: 2, textAlign: 'right', fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  
  dividerLabel: { fontSize: 11, color: '#9CA3AF', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 8, marginTop: 4, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  dividerLine: { borderTopWidth: 1, marginVertical: 12 },

  idBadge: { backgroundColor: '#DCFCE7', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 6 },
  idBadgeText: { color: '#166534', fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, fontSize: 12 },

  auditRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 8, borderBottomWidth: 1, borderBottomColor: '#F9FAFB' },
  auditLabel: { fontSize: 12, color: '#6B7280' },
  auditVal: { fontSize: 12, color: '#111827', fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },

  modalOverlay: { flex: 1, backgroundColor: 'rgba(0, 0, 0, 0.5)', justifyContent: 'center', alignItems: 'center', padding: 20 },
  modalContent: { width: '100%', borderRadius: 16, padding: 24, alignItems: 'center' },
  pillButton: { flex: 1, borderWidth: 1, borderRadius: 999, paddingVertical: 10, alignItems: "center", justifyContent: "center" },
  modalLabel: { fontSize: 12, color: "#6B7280", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, marginBottom: 6 },
  modalPicker: { borderWidth: 1, borderRadius: 12, paddingHorizontal: 12, paddingVertical: 12, flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  modalInputWrap: { borderWidth: 1, borderRadius: 12, paddingHorizontal: 12, paddingVertical: 10 },
});
