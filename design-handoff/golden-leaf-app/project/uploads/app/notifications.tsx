import React, { useCallback, useEffect, useMemo, useState, useRef } from "react";
import { FlatList, Platform, Pressable, RefreshControl, StyleSheet, TextInput, View, Modal, ScrollView, Animated } from "react-native";
import { useRouter } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { Swipeable } from "react-native-gesture-handler";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { LeafLoader } from "@/components/LeafLoader";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiFetchJson } from "@/lib/inspection-storage";
import { readCacheItems, writeCache } from "@/lib/offline-cache";

type Notification = {
  id: string;
  channel: string;
  subject: string;
  message: string;
  status: string;
  reference: string | null;
  created_at: string;
};

type NotifState = {
  read: string[];
  fav: string[];
  archive: string[];
  deleted: string[];
};

export default function NotificationsScreen() {
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const router = useRouter();
  const insets = useSafeAreaInsets();

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState("all");

  const [notifState, setNotifState] = useState<NotifState>({ read: [], fav: [], archive: [], deleted: [] });
  
  const [selectedNotif, setSelectedNotif] = useState<Notification | null>(null);
  const [modalVisible, setModalVisible] = useState(false);

  // Keep references to close swiped items when another is opened
  const swipeableRowRefs = useRef<{ [key: string]: Swipeable | null }>({});

  const STORAGE_KEY = "tbz_notif_state_v1";

  const loadState = async () => {
    try {
      const raw = await AsyncStorage.getItem(STORAGE_KEY);
      if (raw) setNotifState(JSON.parse(raw));
    } catch {}
  };

  const saveState = async (newState: NotifState) => {
    try {
      await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(newState));
    } catch {}
  };

  const updateStateArray = (key: keyof NotifState, id: string) => {
    setNotifState(prev => {
      const arr = [...prev[key]];
      const index = arr.indexOf(id);
      if (index >= 0) arr.splice(index, 1);
      else arr.push(id);
      const newState = { ...prev, [key]: arr };
      saveState(newState);
      return newState;
    });
  };

  const markAsRead = (id: string) => {
    setNotifState(prev => {
      if (prev.read.includes(id)) return prev;
      const newState = { ...prev, read: [...prev.read, id] };
      saveState(newState);
      return newState;
    });
  };

  const fetchNotifications = async () => {
    try {
      const cached = await readCacheItems<Notification>("notifications");
      if (cached.length > 0 && notifications.length === 0) setNotifications(cached);

      const resp = await apiFetchJson("/api/v1/notifications/my/");
      if (resp.ok) {
        const data = JSON.parse(resp.body);
        const rows = Array.isArray(data) ? data : (data.results ?? []);
        setNotifications(rows);
        if (Array.isArray(rows) && rows.length > 0) void writeCache("notifications", rows);
      }
    } catch (e) {
      console.log("Failed to load notifications", e);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadState();
    fetchNotifications();
  }, []);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    fetchNotifications();
  }, []);

  const filteredData = useMemo(() => {
    return notifications.filter((notif) => {
      if (notifState.deleted.includes(notif.id)) return false;

      const isArchive = notifState.archive.includes(notif.id);
      const isFav = notifState.fav.includes(notif.id);

      if (activeFilter === "archive" && !isArchive) return false;
      if (activeFilter === "favorite" && !isFav) return false;
      if (activeFilter === "all" && isArchive) return false;

      const q = searchQuery.toLowerCase();
      const matchesSearch =
        !q ||
        (notif.subject || "").toLowerCase().includes(q) ||
        (notif.message || "").toLowerCase().includes(q) ||
        (notif.reference || "").toLowerCase().includes(q);
      
      return matchesSearch;
    });
  }, [notifications, activeFilter, searchQuery, notifState]);

  const stats = useMemo(() => {
    const allCount = notifications.filter(n => !notifState.deleted.includes(n.id) && !notifState.archive.includes(n.id)).length;
    const archiveCount = notifications.filter(n => !notifState.deleted.includes(n.id) && notifState.archive.includes(n.id)).length;
    const favCount = notifications.filter(n => !notifState.deleted.includes(n.id) && notifState.fav.includes(n.id)).length;
    return { allCount, archiveCount, favCount };
  }, [notifications, notifState]);

  const getChannelIcon = (channel: string) => {
    switch (channel) {
      case "EMAIL": return "email";
      case "SMS": return "smartphone";
      default: return "notifications";
    }
  };
  const getChannelColor = (channel: string) => {
    switch (channel) {
      case "EMAIL": return "#3B82F6";
      case "SMS": return "#10B981";
      default: return "#F59E0B";
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "SENT": return "#10B981";
      case "PENDING": return "#F59E0B";
      case "FAILED": return "#EF4444";
      default: return "#9CA3AF";
    }
  };

  const openDetail = (item: Notification) => {
    markAsRead(item.id);
    setSelectedNotif(item);
    setModalVisible(true);
  };

  const renderRightActions = (item: Notification, dragX: Animated.AnimatedInterpolation<number>) => {
    const isArchive = notifState.archive.includes(item.id);
    const scale = dragX.interpolate({ inputRange: [-100, 0], outputRange: [1, 0.9], extrapolate: 'clamp' });

    return (
      <View style={styles.swipeActionsContainer}>
        <Animated.View style={{ transform: [{ scale }] }}>
          <RectButton
            style={[styles.swipeAction, { backgroundColor: Colors[theme].muted }]}
            onPress={() => {
              swipeableRowRefs.current[item.id]?.close();
              updateStateArray('archive', item.id);
            }}
          >
            <MaterialIcons name={isArchive ? "unarchive" : "archive"} size={24} color="#FFF" />
          </RectButton>
        </Animated.View>
        <Animated.View style={{ transform: [{ scale }] }}>
          <RectButton
            style={[styles.swipeAction, { backgroundColor: '#EF4444' }]}
            onPress={() => {
              swipeableRowRefs.current[item.id]?.close();
              updateStateArray('deleted', item.id);
            }}
          >
            <MaterialIcons name="delete-outline" size={24} color="#FFF" />
          </RectButton>
        </Animated.View>
      </View>
    );
  };

  // RectButton wrapper for custom swipe actions
  const RectButton = ({ onPress, style, children }: any) => (
    <Pressable onPress={onPress} style={style}>
      {children}
    </Pressable>
  );

  const formatTimeAgo = (dateStr: string) => {
    const diff = new Date().getTime() - new Date(dateStr).getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    if (days > 0) return `${days}d ago`;
    const hours = Math.floor(diff / (1000 * 60 * 60));
    if (hours > 0) return `${hours}h ago`;
    const mins = Math.floor(diff / (1000 * 60));
    return `${Math.max(1, mins)}m ago`;
  };

  const renderItem = ({ item }: { item: Notification }) => {
    const isUnread = !notifState.read.includes(item.id);
    const isFav = notifState.fav.includes(item.id);

    return (
      <Swipeable
        ref={ref => { swipeableRowRefs.current[item.id] = ref; }}
        renderRightActions={(_, dragX) => renderRightActions(item, dragX)}
        overshootRight={false}
      >
        <Pressable 
          style={[styles.card, { backgroundColor: Colors[theme].background, borderBottomColor: Colors[theme].border }]}
          onPress={() => openDetail(item)}
        >
          {isUnread && <View style={styles.unreadIndicator} />}

          <View style={[styles.iconWrap, { backgroundColor: getChannelColor(item.channel) + "1A" }]}>
            <MaterialIcons name={getChannelIcon(item.channel) as any} size={20} color={getChannelColor(item.channel)} />
          </View>

          <View style={styles.contentWrap}>
            <View style={styles.titleRowFlex}>
              <ThemedText style={[styles.subject, isUnread && styles.subjectUnread]} numberOfLines={1}>
                {item.subject || "Notification"}
              </ThemedText>
              <ThemedText style={styles.timeText} numberOfLines={1} ellipsizeMode="tail">
                {formatTimeAgo(item.created_at)}
              </ThemedText>
            </View>

            <ThemedText style={[styles.messagePreview, isUnread && styles.messageUnread]} numberOfLines={2}>
              {item.message}
            </ThemedText>

            <View style={styles.cardFooter}>
              <View style={styles.badgesWrap}>
                <View style={[styles.statusBadge, { backgroundColor: getStatusColor(item.status) + "1A" }]}>
                  <ThemedText style={[styles.statusBadgeText, { color: getStatusColor(item.status) }]} numberOfLines={1} ellipsizeMode="tail">
                    {item.status}
                  </ThemedText>
                </View>
                {item.reference && (
                  <View style={styles.refBadge}>
                    <MaterialIcons name="tag" size={12} color={Colors[theme].muted} />
                    <ThemedText style={styles.refText} numberOfLines={1} ellipsizeMode="tail">
                      {item.reference}
                    </ThemedText>
                  </View>
                )}
              </View>
              <Pressable onPress={() => updateStateArray('fav', item.id)} style={styles.favBtn} hitSlop={15}>
                <MaterialIcons name={isFav ? "star" : "star-outline"} size={20} color={isFav ? "#F59E0B" : Colors[theme].muted} />
              </Pressable>
            </View>
          </View>
        </Pressable>
      </Swipeable>
    );
  };

  return (
    <ThemedView style={styles.container}>
      {/* Search Header */}
      <View style={[styles.searchHeader, { backgroundColor: Colors[theme].background, borderBottomColor: Colors[theme].border }]}>
        <View style={[styles.searchField, { backgroundColor: Colors[theme].surface }]}>
          <MaterialIcons name="search" size={20} color={Colors[theme].muted} style={styles.searchIcon} />
          <TextInput
            style={[styles.searchInput, { color: Colors[theme].text }]}
            placeholder="Search notifications..."
            placeholderTextColor={Colors[theme].muted}
            value={searchQuery}
            onChangeText={setSearchQuery}
            autoCorrect={false}
          />
          {searchQuery.length > 0 && (
            <Pressable onPress={() => setSearchQuery("")} style={styles.clearBtn} hitSlop={10}>
              <MaterialIcons name="cancel" size={18} color={Colors[theme].muted} />
            </Pressable>
          )}
        </View>
      </View>

      {/* Segmented Tab Bar */}
      <View style={[styles.tabBar, { borderBottomColor: Colors[theme].border }]}>
        {[
          { id: "all", label: "All", count: stats.allCount },
          { id: "archive", label: "Archive", count: stats.archiveCount },
          { id: "favorite", label: "Favorite", count: stats.favCount }
        ].map(tab => {
          const isActive = activeFilter === tab.id;
          return (
            <Pressable
              key={tab.id}
              style={[styles.tabItem, isActive && { borderBottomColor: Colors[theme].primary }]}
              onPress={() => setActiveFilter(tab.id)}
            >
              <ThemedText style={[styles.tabLabel, isActive && { color: Colors[theme].primary, fontWeight: '600' }]}>
                {tab.label} {tab.count > 0 ? `(${tab.count})` : ''}
              </ThemedText>
            </Pressable>
          );
        })}
      </View>

      <FlatList
        data={filteredData}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.listContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        renderItem={renderItem}
        ListEmptyComponent={
          !loading ? (
            <View style={styles.emptyState}>
              <MaterialIcons name="notifications-none" size={56} color={Colors[theme].muted} style={{ opacity: 0.5 }} />
              <ThemedText style={styles.emptyTitle}>You&apos;re all caught up!</ThemedText>
              <ThemedText style={styles.emptySub}>No notifications found right now.</ThemedText>
            </View>
          ) : (
            <View style={{ marginTop: 40, alignItems: "center" }}>
              <LeafLoader size={42} />
            </View>
          )
        }
      />

      {/* Modern Bottom Sheet Detail Modal */}
      <Modal animationType="slide" transparent={true} visible={modalVisible} onRequestClose={() => setModalVisible(false)}>
        <Pressable style={styles.bottomSheetOverlay} onPress={() => setModalVisible(false)}>
          <Pressable
            onPress={() => {}}
            style={[
              styles.bottomSheetContent,
              { backgroundColor: Colors[theme].surface, paddingBottom: Math.max(16, insets.bottom + 12) },
            ]}
          >
            <View style={styles.dragHandle} />
            
            <View style={styles.sheetHeader}>
              <View style={[styles.sheetIconWrap, { backgroundColor: getChannelColor(selectedNotif?.channel || "") + "1A" }]}>
                <MaterialIcons name={getChannelIcon(selectedNotif?.channel || "") as any} size={24} color={getChannelColor(selectedNotif?.channel || "")} />
              </View>
              <View style={styles.sheetTitleFlex}>
                <ThemedText style={styles.sheetTitle}>{selectedNotif?.subject}</ThemedText>
                <ThemedText style={styles.sheetTime}>
                  {selectedNotif ? new Date(selectedNotif.created_at).toLocaleString() : ""}
                </ThemedText>
              </View>
            </View>

            <ScrollView
              style={styles.sheetBody}
              contentContainerStyle={styles.sheetBodyContent}
              showsVerticalScrollIndicator={false}
            >
              <ThemedText style={styles.sheetMessage}>{selectedNotif?.message}</ThemedText>
            </ScrollView>

            <View style={styles.sheetMetaWrap}>
              <View style={[styles.sheetBadge, { backgroundColor: getStatusColor(selectedNotif?.status || "") + "1A" }]}>
                <ThemedText style={[styles.sheetBadgeText, { color: getStatusColor(selectedNotif?.status || "") }]} numberOfLines={1} ellipsizeMode="tail">
                  {selectedNotif?.status}
                </ThemedText>
              </View>
              {selectedNotif?.reference && (
                <View style={[styles.sheetBadge, { backgroundColor: '#F3F4F6' }]}>
                  <MaterialIcons name="tag" size={14} color="#6B7280" style={{ marginRight: 4 }} />
                  <ThemedText style={[styles.sheetBadgeText, { color: "#6B7280" }]} numberOfLines={1} ellipsizeMode="tail">
                    {selectedNotif.reference}
                  </ThemedText>
                </View>
              )}
            </View>
          </Pressable>
        </Pressable>
      </Modal>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  // Header & Search
  searchHeader: {
    paddingHorizontal: 16,
    paddingTop: Platform.OS === 'ios' ? 60 : 40,
    paddingBottom: 12,
  },
  searchField: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 12,
    paddingHorizontal: 12,
    height: 44,
  },
  searchIcon: { marginRight: 8 },
  searchInput: { flex: 1, fontSize: 16 },
  clearBtn: { padding: 4 },
  // Tab Bar
  tabBar: {
    flexDirection: 'row',
    borderBottomWidth: StyleSheet.hairlineWidth,
    paddingHorizontal: 8,
  },
  tabItem: {
    flex: 1,
    paddingVertical: 14,
    alignItems: 'center',
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  tabLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: '#6B7280',
  },
  // List
  listContent: { paddingBottom: 60 },
  card: {
    flexDirection: 'row',
    padding: 16,
    borderBottomWidth: StyleSheet.hairlineWidth,
    position: 'relative',
    overflow: "hidden",
  },
  unreadIndicator: {
    position: 'absolute',
    top: 16,
    left: 8,
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#3B82F6',
  },
  iconWrap: {
    width: 44,
    height: 44,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  contentWrap: { flex: 1, justifyContent: 'center' },
  titleRowFlex: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 4,
  },
  subject: {
    flex: 1,
    fontSize: 16,
    fontWeight: '500',
    marginRight: 8,
  },
  subjectUnread: { fontWeight: '700' },
  timeText: {
    fontSize: 12,
    color: '#9CA3AF',
    marginTop: 2,
  },
  messagePreview: {
    fontSize: 14,
    color: '#6B7280',
    lineHeight: 20,
    marginBottom: 8,
  },
  messageUnread: { color: '#374151' },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  badgesWrap: { flexDirection: 'row', gap: 8, flexShrink: 1, flexWrap: "wrap", flex: 1 },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  statusBadgeText: { fontSize: 10, fontWeight: '700' },
  refBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    flexShrink: 1,
    maxWidth: "70%",
  },
  refText: { fontSize: 12, color: '#9CA3AF', flexShrink: 1 },
  favBtn: { marginLeft: 16 },
  // Swipe Actions
  swipeActionsContainer: {
    flexDirection: 'row',
    width: 140, // Two buttons worth of width
  },
  swipeAction: {
    width: 70,
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
  },
  // Empty State
  emptyState: {
    alignItems: 'center',
    paddingTop: 80,
    paddingHorizontal: 32,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginTop: 16,
    marginBottom: 8,
  },
  emptySub: {
    fontSize: 14,
    color: '#6B7280',
    textAlign: 'center',
  },
  // Bottom Sheet Modal
  bottomSheetOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  bottomSheetContent: {
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingHorizontal: 20,
    paddingTop: 12,
    paddingBottom: 16,
    maxHeight: '85%',
  },
  dragHandle: {
    width: 40,
    height: 4,
    backgroundColor: '#D1D5DB',
    borderRadius: 2,
    alignSelf: 'center',
    marginBottom: 20,
  },
  sheetHeader: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginBottom: 20,
  },
  sheetIconWrap: {
    width: 48,
    height: 48,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  sheetTitleFlex: { flex: 1, justifyContent: 'center' },
  sheetTitle: { fontSize: 18, fontWeight: '700', marginBottom: 4 },
  sheetTime: { fontSize: 13, color: '#6B7280' },
  sheetBody: { marginBottom: 24 },
  sheetBodyContent: { paddingBottom: 4 },
  sheetMessage: { fontSize: 16, lineHeight: 24, color: '#374151' },
  sheetMetaWrap: { flexDirection: 'row', gap: 12 },
  sheetBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
  },
  sheetBadgeText: { fontSize: 12, fontWeight: '600' },
});
