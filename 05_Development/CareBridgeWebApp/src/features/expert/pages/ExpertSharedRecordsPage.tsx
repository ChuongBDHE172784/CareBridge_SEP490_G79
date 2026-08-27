import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ExpertChecklistFormModal } from '../components/ExpertChecklistFormModal';
import {
  fetchExpertSharedRecords,
  editChecklistItemInSharedRecord,
  deleteChecklistItemFromSharedRecord,
  getTaskOriginCategory,
  type SharedRecordEntry,
  type HealthMetricsShareData,
  type HealthMetricItem,
  type ChecklistShareData,
  type ChecklistItemShareData,
} from '../services/expertSharedRecordsService';

type TabType = 'ALL' | 'HEALTH_METRICS' | 'CHECKLIST';
type AlertFilterType = 'ALL' | 'CRITICAL' | 'WARNING' | 'NORMAL';
type TimeRangeFilterType = 'ALL' | '7D' | '14D' | '30D' | '90D';

export interface MotherSummaryCardData {
  motherUserId: string;
  conversationId: string;
  motherName: string;
  motherAvatar?: string;
  motherPhone?: string;
  lastActiveAt: string;
  gestationalWeek?: number;
  overallAlertLevel: 'CRITICAL' | 'WARNING' | 'NORMAL';
  latestHealthRecord?: SharedRecordEntry;
  latestChecklistRecord?: SharedRecordEntry;
  allHealthRecords: SharedRecordEntry[];
  allChecklistRecords: SharedRecordEntry[];
}

export default function ExpertSharedRecordsPage() {
  const navigate = useNavigate();
  const [records, setRecords] = useState<SharedRecordEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<TabType>('ALL');
  const [alertFilter, setAlertFilter] = useState<AlertFilterType>('ALL');
  const [timeRangeFilter, setTimeRangeFilter] = useState<TimeRangeFilterType>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Track the active sub-tab for each mother card: 'HEALTH' or 'CHECKLIST'
  const [cardTabMap, setCardTabMap] = useState<Record<string, 'HEALTH' | 'CHECKLIST'>>({});

  // Modals for deep inspection
  const [selectedHealthModal, setSelectedHealthModal] = useState<{
    motherName: string;
    conversationId: string;
    data: HealthMetricsShareData;
  } | null>(null);
  const [selectedChecklistModal, setSelectedChecklistModal] = useState<{
    motherName: string;
    conversationId: string;
    data: ChecklistShareData;
  } | null>(null);
  const [checklistModalTab, setChecklistModalTab] = useState<'HISTORY' | 'CURRENT' | 'FUTURE'>('CURRENT');
  const [checklistModalOriginFilter, setChecklistModalOriginFilter] = useState<'ALL' | 'CAREBRIDGE' | 'PERSONAL'>('ALL');
  const [cardOriginFilters, setCardOriginFilters] = useState<Record<string, 'ALL' | 'CAREBRIDGE' | 'PERSONAL'>>({});

  // Checklist Item Customization Modals & State
  const [taskFormModal, setTaskFormModal] = useState<{
    isOpen: boolean;
    mode: 'ADD' | 'EDIT';
    targetGroup: 'CURRENT' | 'FUTURE' | 'HISTORY';
    itemIndex?: number;
    text: string;
    category: string;
    timeLabel?: string;
    completed: boolean;
    doctorNote: string;
    sourceUrl?: string;
    conversationId: string;
    checklistData: ChecklistShareData;
    motherName: string;
  } | null>(null);

  const [deleteConfirmModal, setDeleteConfirmModal] = useState<{
    isOpen: boolean;
    targetGroup: 'CURRENT' | 'FUTURE' | 'HISTORY';
    itemIndex: number;
    itemText: string;
    conversationId: string;
    checklistData: ChecklistShareData;
  } | null>(null);

  const [savingTask, setSavingTask] = useState(false);
  const [toastMessage, setToastMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const showToast = (text: string, type: 'success' | 'error' = 'success') => {
    setToastMessage({ text, type });
    setTimeout(() => {
      setToastMessage(null);
    }, 3500);
  };

  const handleTaskSavedFromModal = (updatedData: ChecklistShareData) => {
    const convId = taskFormModal?.conversationId;
    if (convId) {
      setRecords((prev) =>
        prev.map((rec) =>
          rec.conversationId === convId && rec.type === 'CHECKLIST'
            ? { ...rec, checklistData: updatedData, createdAt: new Date().toISOString() }
            : rec
        )
      );
    }
    if (selectedChecklistModal && selectedChecklistModal.conversationId === convId) {
      setSelectedChecklistModal((prev) => (prev ? { ...prev, data: updatedData } : null));
    }
    showToast(
      taskFormModal?.mode === 'ADD'
        ? 'Đã thêm việc cần làm vào lộ trình của mẹ bầu!'
        : 'Đã cập nhật việc cần làm thành công!'
    );
    setTaskFormModal(null);
    setTimeout(() => {
      loadData(true);
    }, 400);
  };

  const handleDeleteTask = async () => {
    if (!deleteConfirmModal) return;
    setSavingTask(true);
    try {
      const updatedData = await deleteChecklistItemFromSharedRecord(
        deleteConfirmModal.conversationId,
        deleteConfirmModal.checklistData,
        deleteConfirmModal.targetGroup,
        deleteConfirmModal.itemIndex,
        undefined,
        deleteConfirmModal.itemText
      );

      showToast('Đã xóa việc cần làm khỏi lộ trình của mẹ bầu');
      if (selectedChecklistModal && selectedChecklistModal.conversationId === deleteConfirmModal.conversationId) {
        setSelectedChecklistModal((prev) => (prev ? { ...prev, data: updatedData } : null));
      }

      setDeleteConfirmModal(null);
      loadData(true);
    } catch (err) {
      console.error('Failed to delete checklist item', err);
      showToast('Có lỗi xảy ra khi xóa việc cần làm', 'error');
    } finally {
      setSavingTask(false);
    }
  };

  const handleToggleTaskStatus = async (
    conversationId: string,
    checklistData: ChecklistShareData,
    targetGroup: 'CURRENT' | 'FUTURE' | 'HISTORY',
    itemIndex: number,
    item: ChecklistItemShareData
  ) => {
    try {
      const updatedItem: ChecklistItemShareData = {
        ...item,
        completed: !item.completed,
      };
      const updatedData = await editChecklistItemInSharedRecord(
        conversationId,
        checklistData,
        targetGroup,
        itemIndex,
        updatedItem
      );
      if (selectedChecklistModal && selectedChecklistModal.conversationId === conversationId) {
        setSelectedChecklistModal((prev) => (prev ? { ...prev, data: updatedData } : null));
      }
      loadData(true);
    } catch (err) {
      console.error('Failed to toggle task status', err);
    }
  };

  const loadData = async (isBackground = false) => {
    if (!isBackground) setLoading(true);
    try {
      const data = await fetchExpertSharedRecords();
      setRecords(data);
    } catch (err) {
      console.error('Error loading shared records', err);
    } finally {
      if (!isBackground) setLoading(false);
    }
  };

  useEffect(() => {
    loadData();

    // Auto-sync every 8 seconds in background for real-time live sync
    const interval = setInterval(() => {
      loadData(true);
    }, 8000);

    const onFocus = () => loadData(true);
    window.addEventListener('focus', onFocus);

    return () => {
      clearInterval(interval);
      window.removeEventListener('focus', onFocus);
    };
  }, []);

  // Group records by Mother Account (1 card per user account)
  const motherCards: MotherSummaryCardData[] = useMemo(() => {
    const map = new Map<string, MotherSummaryCardData>();

    for (const record of records) {
      const key = record.motherUserId || record.conversationId;
      let existing = map.get(key);

      if (!existing) {
        existing = {
          motherUserId: record.motherUserId,
          conversationId: record.conversationId,
          motherName: record.motherName,
          motherAvatar: record.motherAvatar,
          motherPhone: record.motherPhone,
          lastActiveAt: record.createdAt,
          gestationalWeek: record.healthData?.gestationalWeek || record.checklistData?.gestationalWeek,
          overallAlertLevel: 'NORMAL',
          allHealthRecords: [],
          allChecklistRecords: [],
        };
        map.set(key, existing);
      }

      // Update latest timestamp and conversationId
      if (new Date(record.createdAt).getTime() > new Date(existing.lastActiveAt).getTime()) {
        existing.lastActiveAt = record.createdAt;
        existing.conversationId = record.conversationId;
      }

      // Update gestational week
      const gw = record.healthData?.gestationalWeek || record.checklistData?.gestationalWeek;
      if (gw && (!existing.gestationalWeek || gw > existing.gestationalWeek)) {
        existing.gestationalWeek = gw;
      }

      // Collect records
      if (record.type === 'HEALTH_METRICS') {
        existing.allHealthRecords.push(record);
        if (
          !existing.latestHealthRecord ||
          new Date(record.createdAt).getTime() > new Date(existing.latestHealthRecord.createdAt).getTime()
        ) {
          existing.latestHealthRecord = record;
        }
      } else if (record.type === 'CHECKLIST') {
        existing.allChecklistRecords.push(record);
        if (
          !existing.latestChecklistRecord ||
          new Date(record.createdAt).getTime() > new Date(existing.latestChecklistRecord.createdAt).getTime()
        ) {
          existing.latestChecklistRecord = record;
        }
      }

      // Determine overall alert level (CRITICAL > WARNING > NORMAL)
      if (record.alertLevel === 'CRITICAL') {
        existing.overallAlertLevel = 'CRITICAL';
      } else if (record.alertLevel === 'WARNING' && existing.overallAlertLevel !== 'CRITICAL') {
        existing.overallAlertLevel = 'WARNING';
      }
    }

    // Sort cards by newest activity first
    return Array.from(map.values()).sort(
      (a, b) => new Date(b.lastActiveAt).getTime() - new Date(a.lastActiveAt).getTime()
    );
  }, [records]);

  // Filter mother cards
  const filteredMotherCards = useMemo(() => {
    return motherCards.filter((card) => {
      // Tab filter
      if (activeTab === 'HEALTH_METRICS' && !card.latestHealthRecord) return false;
      if (activeTab === 'CHECKLIST' && !card.latestChecklistRecord) return false;

      // Alert filter
      if (alertFilter !== 'ALL' && card.overallAlertLevel !== alertFilter) return false;

      // Time range filter (based on last active at)
      if (timeRangeFilter !== 'ALL') {
        const now = new Date().getTime();
        const days =
          timeRangeFilter === '7D' ? 7 : timeRangeFilter === '14D' ? 14 : timeRangeFilter === '30D' ? 30 : 90;
        const cutoff = now - days * 24 * 60 * 60 * 1000;
        const recordTime = new Date(card.lastActiveAt).getTime();
        if (recordTime < cutoff) return false;
      }

      // Search query
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase().trim();
        const nameMatch = card.motherName.toLowerCase().includes(query);
        const phoneMatch = (card.motherPhone || '').includes(query);
        const healthNoteMatch = (card.latestHealthRecord?.healthData?.note || '').toLowerCase().includes(query);
        const checklistNoteMatch = (card.latestChecklistRecord?.checklistData?.note || '').toLowerCase().includes(query);
        if (!nameMatch && !phoneMatch && !healthNoteMatch && !checklistNoteMatch) return false;
      }

      return true;
    });
  }, [motherCards, activeTab, alertFilter, timeRangeFilter, searchQuery]);

  // Statistics
  const stats = useMemo(() => {
    const totalUsers = motherCards.length;
    const totalRecords = records.length;
    const criticalCount = motherCards.filter((c) => c.overallAlertLevel === 'CRITICAL').length;
    const warningCount = motherCards.filter((c) => c.overallAlertLevel === 'WARNING').length;

    const checklistPercents: number[] = [];
    motherCards.forEach((c) => {
      if (c.latestChecklistRecord?.checklistData) {
        checklistPercents.push(c.latestChecklistRecord.checklistData.progressPercent);
      }
    });

    const avgChecklistProgress =
      checklistPercents.length > 0
        ? Math.round(checklistPercents.reduce((acc, curr) => acc + curr, 0) / checklistPercents.length)
        : 0;

    return {
      totalUsers,
      totalRecords,
      criticalCount,
      warningCount,
      avgChecklistProgress,
    };
  }, [motherCards, records]);

  const getMetricIcon = (code: string) => {
    switch (code) {
      case 'BLOOD_PRESSURE':
        return 'favorite';
      case 'BLOOD_GLUCOSE':
        return 'water_drop';
      case 'BMI':
      case 'WEIGHT':
        return 'monitor_weight';
      case 'MATERNAL_HEART_RATE':
      case 'HEART_RATE':
        return 'monitor_heart';
      case 'TEMPERATURE':
        return 'thermostat';
      case 'FETAL_MOVEMENT':
      case 'FETAL_MOVEMENT_SESSION':
        return 'child_care';
      case 'SPO2':
        return 'air';
      default:
        return 'health_and_safety';
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'CRITICAL':
        return <span className="px-2 py-0.5 text-xs font-bold rounded-md bg-rose-100 text-rose-800">Nguy hiểm</span>;
      case 'WARNING':
        return <span className="px-2 py-0.5 text-xs font-bold rounded-md bg-amber-100 text-amber-800">Cần lưu ý</span>;
      default:
        return <span className="px-2 py-0.5 text-xs font-bold rounded-md bg-emerald-100 text-emerald-800">Bình thường</span>;
    }
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <span className="material-symbols-outlined text-3xl text-primary">monitoring</span>
            <h1 className="text-2xl font-bold text-on-surface tracking-tight m-0">
              Quản lý Chỉ số Sức khỏe & Checklist chia sẻ
            </h1>
          </div>
          <p className="text-sm text-on-surface-variant mt-1 mb-0">
            Theo dõi dữ liệu sinh hiệu và tiến độ checklist thai kỳ mới nhất của từng mẹ bầu
          </p>
        </div>

        <button
          type="button"
          onClick={() => loadData(false)}
          disabled={loading}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-surface border border-outline-variant text-sm font-semibold text-on-surface hover:bg-surface-container-low transition-colors shadow-xs cursor-pointer"
        >
          <span className={`material-symbols-outlined text-lg ${loading ? 'animate-spin' : ''}`}>refresh</span>
          Làm mới
        </button>
      </div>

      {/* KPI Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-4 rounded-2xl bg-surface border border-outline-variant/70 shadow-xs flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-xl bg-primary/10 text-primary flex items-center justify-center shrink-0">
            <span className="material-symbols-outlined text-2xl">pregnant_woman</span>
          </div>
          <div>
            <p className="text-xs text-on-surface-variant font-medium m-0">Mẹ bầu đang theo dõi</p>
            <p className="text-2xl font-bold text-on-surface m-0 mt-0.5">
              {stats.totalUsers}{' '}
              <span className="text-xs font-normal text-on-surface-variant">({stats.totalRecords} lượt gửi)</span>
            </p>
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-surface border border-outline-variant/70 shadow-xs flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-xl bg-rose-100 text-rose-700 flex items-center justify-center shrink-0">
            <span className="material-symbols-outlined text-2xl">warning</span>
          </div>
          <div>
            <p className="text-xs text-on-surface-variant font-medium m-0">Ca có cảnh báo / Bất thường</p>
            <p className="text-2xl font-bold text-rose-700 m-0 mt-0.5">
              {stats.criticalCount + stats.warningCount}
              <span className="text-xs font-normal text-on-surface-variant ml-1.5">
                ({stats.criticalCount} nguy cơ cao)
              </span>
            </p>
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-surface border border-outline-variant/70 shadow-xs flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
            <span className="material-symbols-outlined text-2xl">task_alt</span>
          </div>
          <div>
            <p className="text-xs text-on-surface-variant font-medium m-0">Tiến độ Checklist TB</p>
            <p className="text-2xl font-bold text-emerald-700 m-0 mt-0.5">{stats.avgChecklistProgress}%</p>
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-surface border border-outline-variant/70 shadow-xs flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-xl bg-sky-100 text-sky-700 flex items-center justify-center shrink-0">
            <span className="material-symbols-outlined text-2xl">sync</span>
          </div>
          <div>
            <p className="text-xs text-on-surface-variant font-medium m-0">Đồng bộ Thời gian thực</p>
            <p className="text-2xl font-bold text-sky-700 m-0 mt-0.5 flex items-center gap-1.5">
              <span>Live Sync</span>
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse inline-block" />
            </p>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="p-4 rounded-2xl bg-surface border border-outline-variant/70 shadow-xs space-y-4">
        <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
          {/* Main Tabs */}
          <div className="inline-flex p-1 bg-surface-container-low rounded-xl border border-outline-variant/50">
            <button
              type="button"
              onClick={() => setActiveTab('ALL')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                activeTab === 'ALL'
                  ? 'bg-surface text-primary shadow-xs'
                  : 'text-on-surface-variant hover:text-on-surface'
              }`}
            >
              Tất cả tài khoản ({motherCards.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('HEALTH_METRICS')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                activeTab === 'HEALTH_METRICS'
                  ? 'bg-surface text-primary shadow-xs'
                  : 'text-on-surface-variant hover:text-on-surface'
              }`}
            >
              Có chỉ số sức khỏe ({motherCards.filter((c) => c.latestHealthRecord).length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('CHECKLIST')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                activeTab === 'CHECKLIST'
                  ? 'bg-surface text-primary shadow-xs'
                  : 'text-on-surface-variant hover:text-on-surface'
              }`}
            >
              Có checklist ({motherCards.filter((c) => c.latestChecklistRecord).length})
            </button>
          </div>

          {/* Right filters: Time range, Alert filter & Search */}
          <div className="flex flex-wrap items-center gap-2.5">
            <select
              value={timeRangeFilter}
              onChange={(e) => setTimeRangeFilter(e.target.value as TimeRangeFilterType)}
              className="px-3 py-1.5 rounded-lg border border-outline-variant bg-surface text-xs font-medium text-on-surface focus:outline-none focus:border-primary cursor-pointer"
            >
              <option value="ALL">Mọi mốc thời gian</option>
              <option value="7D">7 ngày gần đây</option>
              <option value="14D">14 ngày gần đây</option>
              <option value="30D">30 ngày gần đây</option>
              <option value="90D">3 tháng qua</option>
            </select>

            <select
              value={alertFilter}
              onChange={(e) => setAlertFilter(e.target.value as AlertFilterType)}
              className="px-3 py-1.5 rounded-lg border border-outline-variant bg-surface text-xs font-medium text-on-surface focus:outline-none focus:border-primary cursor-pointer"
            >
              <option value="ALL">Tất cả mức độ cảnh báo</option>
              <option value="CRITICAL">🔴 Nguy hiểm (Critical)</option>
              <option value="WARNING">🟡 Cần lưu ý (Warning)</option>
              <option value="NORMAL">🟢 Bình thường (Normal)</option>
            </select>

            <div className="relative min-w-[220px]">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-lg text-outline">
                search
              </span>
              <input
                type="text"
                placeholder="Tìm tên mẹ bầu, SĐT, ghi chú..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-1.5 rounded-lg border border-outline-variant bg-surface text-xs text-on-surface placeholder:text-outline focus:outline-none focus:border-primary"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Main Unified Patient Cards Feed */}
      {loading ? (
        <div className="py-20 text-center text-on-surface-variant">
          <div className="inline-block w-8 h-8 border-3 border-primary border-t-transparent rounded-full animate-spin mb-3" />
          <p className="text-sm font-medium">Đang tải hồ sơ chỉ số và checklist của các mẹ bầu...</p>
        </div>
      ) : filteredMotherCards.length === 0 ? (
        <div className="p-12 text-center rounded-2xl bg-surface border border-dashed border-outline-variant">
          <span className="material-symbols-outlined text-5xl text-outline mb-2">folder_open</span>
          <h3 className="text-base font-bold text-on-surface m-0">Không tìm thấy hồ sơ mẹ bầu nào</h3>
          <p className="text-xs text-on-surface-variant mt-1">
            Chưa có mẹ bầu nào chia sẻ chỉ số hoặc checklist theo tiêu chí tìm kiếm của bạn.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {filteredMotherCards.map((card) => {
            const cardKey = card.motherUserId || card.conversationId;
            // Default active sub-tab: if has health, default HEALTH; otherwise CHECKLIST
            const currentSubTab =
              cardTabMap[cardKey] || (card.latestHealthRecord ? 'HEALTH' : 'CHECKLIST');

            const healthData = card.latestHealthRecord?.healthData;
            const checklistData = card.latestChecklistRecord?.checklistData;

            return (
              <div
                key={cardKey}
                className={`rounded-2xl bg-surface border shadow-sm overflow-hidden flex flex-col justify-between transition-all ${
                  card.overallAlertLevel === 'CRITICAL'
                    ? 'border-rose-300 ring-1 ring-rose-200'
                    : card.overallAlertLevel === 'WARNING'
                    ? 'border-amber-300'
                    : 'border-outline-variant/70 hover:border-primary/50'
                }`}
              >
                <div>
                  {/* Card Top: Patient Header */}
                  <div className="p-4 border-b border-outline-variant/40 bg-surface-container-low/40 flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-11 h-11 rounded-full bg-primary-container text-primary flex items-center justify-center font-bold text-base shadow-xs shrink-0">
                        {card.motherAvatar ? (
                          <img
                            src={card.motherAvatar}
                            alt={card.motherName}
                            className="w-full h-full rounded-full object-cover"
                          />
                        ) : (
                          card.motherName.charAt(0).toUpperCase()
                        )}
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <h3 className="text-sm font-bold text-on-surface truncate m-0">{card.motherName}</h3>
                          <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-pulse" />
                            Live
                          </span>
                        </div>
                        <p className="text-xs text-on-surface-variant m-0 mt-0.5">
                          {[
                            card.gestationalWeek ? `Tuần thai ${card.gestationalWeek}` : null,
                            `Cập nhật gần nhất: ${new Date(card.lastActiveAt).toLocaleDateString('vi-VN', {
                              day: '2-digit',
                              month: '2-digit',
                              year: 'numeric',
                              hour: '2-digit',
                              minute: '2-digit',
                            })}`,
                          ]
                            .filter(Boolean)
                            .join(' · ')}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      {getStatusBadge(card.overallAlertLevel)}
                    </div>
                  </div>

                  {/* Card Sub-Tab Switcher (Chỉ số sức khỏe vs Việc cần làm) */}
                  <div className="px-4 pt-3 pb-2 border-b border-outline-variant/30 bg-surface-container-lowest flex items-center justify-between gap-2">
                    <div className="inline-flex p-0.5 rounded-lg bg-surface-container border border-outline-variant/40 text-xs">
                      <button
                        type="button"
                        onClick={() =>
                          setCardTabMap((prev) => ({
                            ...prev,
                            [cardKey]: 'HEALTH',
                          }))
                        }
                        className={`px-3 py-1 rounded-md font-semibold transition-all cursor-pointer flex items-center gap-1.5 ${
                          currentSubTab === 'HEALTH'
                            ? 'bg-surface text-primary shadow-xs'
                            : 'text-on-surface-variant hover:text-on-surface'
                        }`}
                      >
                        <span className="material-symbols-outlined text-sm">vital_signs</span>
                        Chỉ số sức khỏe
                        {card.allHealthRecords.length > 0 && (
                          <span
                            className={`px-1.5 py-0.2 rounded-full text-[10px] font-bold ${
                              currentSubTab === 'HEALTH'
                                ? 'bg-primary/10 text-primary'
                                : 'bg-surface-container-high text-on-surface-variant'
                            }`}
                          >
                            {card.allHealthRecords.length}
                          </span>
                        )}
                      </button>

                      <button
                        type="button"
                        onClick={() =>
                          setCardTabMap((prev) => ({
                            ...prev,
                            [cardKey]: 'CHECKLIST',
                          }))
                        }
                        className={`px-3 py-1 rounded-md font-semibold transition-all cursor-pointer flex items-center gap-1.5 ${
                          currentSubTab === 'CHECKLIST'
                            ? 'bg-surface text-primary shadow-xs'
                            : 'text-on-surface-variant hover:text-on-surface'
                        }`}
                      >
                        <span className="material-symbols-outlined text-sm">checklist</span>
                        Việc cần làm
                        {card.latestChecklistRecord?.checklistData && (
                          <span
                            className={`px-1.5 py-0.2 rounded-full text-[10px] font-bold ${
                              currentSubTab === 'CHECKLIST'
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-surface-container-high text-on-surface-variant'
                            }`}
                          >
                            {card.latestChecklistRecord.checklistData.progressPercent}%
                          </span>
                        )}
                      </button>
                    </div>

                    <div className="text-[11px] text-on-surface-variant font-medium">
                      {currentSubTab === 'HEALTH'
                        ? healthData?.timeRangeLabel || 'Lần đo mới nhất'
                        : checklistData
                        ? `Xong ${checklistData.completedCount}/${checklistData.totalCount}`
                        : ''}
                    </div>
                  </div>

                  {/* Card Section Content */}
                  <div className="p-4 space-y-3.5 min-h-[170px]">
                    {/* --- HEALTH SUB-TAB CONTENT --- */}
                    {currentSubTab === 'HEALTH' && (
                      <>
                        {healthData ? (
                          <>
                            {/* Vital Metrics Grid */}
                            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                              {healthData.metrics.map((metric: HealthMetricItem) => (
                                <div
                                  key={metric.code}
                                  className={`p-2.5 rounded-xl border flex flex-col justify-between ${
                                    metric.status === 'CRITICAL'
                                      ? 'bg-rose-50/80 border-rose-200'
                                      : metric.status === 'WARNING'
                                      ? 'bg-amber-50/80 border-amber-200'
                                      : 'bg-surface-container-low/50 border-outline-variant/40'
                                  }`}
                                >
                                  <div className="flex items-center gap-1.5 mb-1">
                                    <span className="material-symbols-outlined text-sm text-primary">
                                      {getMetricIcon(metric.code)}
                                    </span>
                                    <span className="text-[11px] font-medium text-on-surface-variant truncate">
                                      {metric.name}
                                    </span>
                                  </div>
                                  <div className="flex items-baseline gap-1">
                                    <span className="text-sm font-bold text-on-surface">{metric.value}</span>
                                    <span className="text-[10px] text-on-surface-variant">{metric.unit}</span>
                                  </div>
                                  {metric.measuredTime && (
                                    <span className="text-[9px] text-outline mt-1">{metric.measuredTime}</span>
                                  )}
                                </div>
                              ))}
                            </div>

                            {/* Note if any */}
                            {healthData.note && (
                              <div className="p-2.5 rounded-xl bg-surface-container-low/60 border border-outline-variant/40 text-xs">
                                <span className="font-semibold text-on-surface">Ghi chú từ mẹ: </span>
                                <span className="text-on-surface-variant italic">{healthData.note}</span>
                              </div>
                            )}
                          </>
                        ) : (
                          <div className="py-8 text-center text-xs text-on-surface-variant bg-surface-container-low/30 rounded-xl border border-dashed border-outline-variant/50">
                            <span className="material-symbols-outlined text-3xl text-outline mb-1">vital_signs</span>
                            <p className="m-0 font-medium">Chưa có dữ liệu chỉ số sức khỏe nào được gửi.</p>
                          </div>
                        )}
                      </>
                    )}

                    {/* --- CHECKLIST SUB-TAB CONTENT --- */}
                    {currentSubTab === 'CHECKLIST' && (
                      <>
                        {checklistData ? (
                          <>
                            {/* Progress bar with Add Task quick action */}
                            <div>
                              <div className="flex items-center justify-between text-xs mb-1">
                                <span className="font-medium text-on-surface-variant text-[11px]">
                                  Tiến độ hoàn thành việc cần làm
                                </span>
                                <div className="flex items-center gap-2">
                                  <span className="font-bold text-primary">
                                    {checklistData.completedCount}/{checklistData.totalCount} việc (
                                    {checklistData.progressPercent}%)
                                  </span>
                                  <button
                                    type="button"
                                    onClick={() =>
                                      setTaskFormModal({
                                        isOpen: true,
                                        mode: 'ADD',
                                        targetGroup: 'CURRENT',
                                        text: '',
                                        category: 'Khám thai & Y tế',
                                        completed: false,
                                        doctorNote: '',
                                        conversationId: card.conversationId,
                                        checklistData,
                                        motherName: card.motherName,
                                      })
                                    }
                                    className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-primary/10 hover:bg-primary/20 text-primary text-[10px] font-bold transition-colors cursor-pointer"
                                  >
                                    <span className="material-symbols-outlined text-xs">add</span>
                                    Thêm việc
                                  </button>
                                </div>
                              </div>
                              <div className="w-full h-2 rounded-full bg-surface-container-highest overflow-hidden">
                                <div
                                  className="h-full bg-primary rounded-full transition-all duration-300"
                                  style={{
                                    width: `${Math.min(100, Math.max(0, checklistData.progressPercent))}%`,
                                  }}
                                />
                              </div>
                            </div>

                            {/* Checklist tasks preview with Edit & Delete actions */}
                            {(() => {
                              const historyTexts = new Set(
                                (checklistData.historyItems || []).map((h) => h.text.trim().toLowerCase())
                              );
                              const currentList = (
                                checklistData.currentItems ||
                                checklistData.items ||
                                []
                              ).filter((item) => !historyTexts.has(item.text.trim().toLowerCase()));

                              const cbCount = currentList.filter(
                                (i) => getTaskOriginCategory(i) === 'CAREBRIDGE'
                              ).length;
                              const personalCount = currentList.filter(
                                (i) => getTaskOriginCategory(i) === 'USER'
                              ).length;
                              const currentOriginFilter = cardOriginFilters[cardKey] || 'ALL';

                              const displayedList = currentList.filter((item) => {
                                const originCat = getTaskOriginCategory(item);
                                const isCareBridge = originCat === 'CAREBRIDGE';
                                if (currentOriginFilter === 'CAREBRIDGE') return isCareBridge;
                                if (currentOriginFilter === 'PERSONAL') return !isCareBridge;
                                return true;
                              });

                              return (
                                <div className="space-y-2">
                                  {/* Origin Filter Bar on Card */}
                                  <div className="flex items-center gap-1.5 flex-wrap pb-1">
                                    <button
                                      type="button"
                                      onClick={() =>
                                        setCardOriginFilters((prev) => ({
                                          ...prev,
                                          [cardKey]: 'ALL',
                                        }))
                                      }
                                      className={`px-2 py-0.5 rounded-md text-[10px] font-bold transition-colors cursor-pointer ${
                                        currentOriginFilter === 'ALL'
                                          ? 'bg-primary text-white shadow-xs'
                                          : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container-high'
                                      }`}
                                    >
                                      Tất cả ({currentList.length})
                                    </button>
                                    <button
                                      type="button"
                                      onClick={() =>
                                        setCardOriginFilters((prev) => ({
                                          ...prev,
                                          [cardKey]: 'CAREBRIDGE',
                                        }))
                                      }
                                      className={`px-2 py-0.5 rounded-md text-[10px] font-bold transition-colors cursor-pointer ${
                                        currentOriginFilter === 'CAREBRIDGE'
                                          ? 'bg-sky-700 text-white shadow-xs'
                                          : 'bg-sky-50 text-sky-800 border border-sky-200 hover:bg-sky-100'
                                      }`}
                                    >
                                      ✨ Gợi ý CareBridge ({cbCount})
                                    </button>
                                    <button
                                      type="button"
                                      onClick={() =>
                                        setCardOriginFilters((prev) => ({
                                          ...prev,
                                          [cardKey]: 'PERSONAL',
                                        }))
                                      }
                                      className={`px-2 py-0.5 rounded-md text-[10px] font-bold transition-colors cursor-pointer ${
                                        currentOriginFilter === 'PERSONAL'
                                          ? 'bg-purple-700 text-white shadow-xs'
                                          : 'bg-purple-50 text-purple-800 border border-purple-200 hover:bg-purple-100'
                                      }`}
                                    >
                                      👤 Việc cá nhân ({personalCount})
                                    </button>
                                  </div>

                                  <div className="space-y-1.5 max-h-60 overflow-y-auto pr-1">
                                    {displayedList.length === 0 ? (
                                      <div className="py-4 text-center text-xs text-on-surface-variant italic">
                                        Không có việc nào phù hợp với bộ lọc.
                                      </div>
                                    ) : (
                                      displayedList.map((item, idx) => {
                                        const originCat = getTaskOriginCategory(item);
                                        return (
                                        <div
                                          key={idx}
                                          className={`group p-2 rounded-xl flex items-center justify-between gap-2 text-xs border transition-all ${
                                            item.completed
                                              ? 'bg-emerald-50/50 border-emerald-200/50 text-emerald-950'
                                              : 'bg-surface-container-low/40 border-outline-variant/30 text-on-surface'
                                          }`}
                                        >
                                          <div
                                            onClick={() =>
                                              handleToggleTaskStatus(
                                                card.conversationId,
                                                checklistData,
                                                'CURRENT',
                                                idx,
                                                item
                                              )
                                            }
                                            className="flex items-center gap-2 min-w-0 cursor-pointer flex-1"
                                          >
                                            <span
                                              className={`material-symbols-outlined text-base shrink-0 transition-transform active:scale-90 ${
                                                item.completed ? 'text-emerald-600' : 'text-outline'
                                              }`}
                                            >
                                              {item.completed ? 'check_circle' : 'radio_button_unchecked'}
                                            </span>
                                            <div className="min-w-0 flex-1 flex items-center gap-1.5 flex-wrap">
                                              <span className="truncate leading-snug">{item.text}</span>
                                              {originCat === 'USER' ? (
                                                <span className="inline-flex items-center gap-0.5 px-1.5 py-0.2 rounded text-[9px] font-bold bg-purple-50 text-purple-800 border border-purple-200 shrink-0">
                                                  <span className="material-symbols-outlined text-[11px] text-purple-600">person</span>
                                                  Việc cá nhân
                                                </span>
                                              ) : (
                                                <span className="inline-flex items-center gap-0.5 px-1.5 py-0.2 rounded text-[9px] font-bold bg-sky-50 text-sky-800 border border-sky-200 shrink-0">
                                                  <span className="material-symbols-outlined text-[11px] text-sky-600">auto_awesome</span>
                                                  Gợi ý CareBridge
                                                </span>
                                              )}
                                            </div>
                                          </div>

                                          <div className="flex items-center gap-1.5 shrink-0">
                                            <span
                                              className={`px-1.5 py-0.2 text-[9px] font-bold rounded ${
                                                item.completed
                                                  ? 'bg-emerald-100 text-emerald-800'
                                                  : 'bg-amber-100 text-amber-800'
                                              }`}
                                            >
                                              {item.completed ? 'Đã xong' : 'Chờ làm'}
                                            </span>

                                            {/* Quick Edit & Delete icons */}
                                            <button
                                              type="button"
                                              title="Chỉnh sửa việc cần làm"
                                              onClick={(e) => {
                                                e.stopPropagation();
                                                setTaskFormModal({
                                                  isOpen: true,
                                                  mode: 'EDIT',
                                                  targetGroup: checklistData.currentItems ? 'CURRENT' : 'HISTORY',
                                                  itemIndex: idx,
                                                  text: item.text,
                                                  category: item.category || 'Khám thai & Y tế',
                                                  timeLabel: item.timeLabel,
                                                  completed: item.completed,
                                                  doctorNote: item.doctorNote || '',
                                                  sourceUrl: item.sourceUrl || '',
                                                  conversationId: card.conversationId,
                                                  checklistData,
                                                  motherName: card.motherName,
                                                });
                                              }}
                                              className="w-6 h-6 rounded flex items-center justify-center text-on-surface-variant hover:text-primary hover:bg-primary/10 transition-colors cursor-pointer"
                                            >
                                              <span className="material-symbols-outlined text-sm">edit</span>
                                            </button>

                                            <button
                                              type="button"
                                              title="Xóa việc cần làm"
                                              onClick={(e) => {
                                                e.stopPropagation();
                                                setDeleteConfirmModal({
                                                  isOpen: true,
                                                  targetGroup: checklistData.currentItems ? 'CURRENT' : 'HISTORY',
                                                  itemIndex: idx,
                                                  itemText: item.text,
                                                  conversationId: card.conversationId,
                                                  checklistData,
                                                });
                                              }}
                                              className="w-6 h-6 rounded flex items-center justify-center text-on-surface-variant hover:text-rose-600 hover:bg-rose-100 transition-colors cursor-pointer"
                                            >
                                              <span className="material-symbols-outlined text-sm">delete</span>
                                            </button>
                                            </div>
                                          </div>
                                        );
                                      })
                                    )}
                                  </div>
                                </div>
                              );
                            })()}

                            {checklistData.note && (
                              <div className="p-2.5 rounded-xl bg-surface-container-low/60 border border-outline-variant/40 text-xs">
                                <span className="font-semibold text-on-surface">Ghi chú checklist: </span>
                                <span className="text-on-surface-variant italic">{checklistData.note}</span>
                              </div>
                            )}
                          </>
                        ) : (
                          <div className="py-8 text-center text-xs text-on-surface-variant bg-surface-container-low/30 rounded-xl border border-dashed border-outline-variant/50">
                            <span className="material-symbols-outlined text-3xl text-outline mb-1">checklist</span>
                            <p className="m-0 font-medium">Chưa có checklist nào được gửi gần đây.</p>
                          </div>
                        )}
                      </>
                    )}
                  </div>
                </div>

                {/* Card Actions Footer */}
                <div className="p-4 border-t border-outline-variant/40 bg-surface flex flex-wrap items-center justify-between gap-2.5">
                  <div className="flex items-center gap-2">
                    {/* View Details Modal Button based on sub-tab */}
                    {currentSubTab === 'HEALTH' && healthData && (
                      <button
                        type="button"
                        onClick={() =>
                          setSelectedHealthModal({
                            motherName: card.motherName,
                            conversationId: card.conversationId,
                            data: healthData,
                          })
                        }
                        className="px-3 py-1.5 rounded-lg border border-outline-variant text-xs font-semibold text-on-surface hover:bg-surface-container-low cursor-pointer transition-colors"
                      >
                        Lịch sử đo chi tiết
                      </button>
                    )}

                    {currentSubTab === 'CHECKLIST' && checklistData && (
                      <button
                        type="button"
                        onClick={() =>
                          setSelectedChecklistModal({
                            motherName: card.motherName,
                            conversationId: card.conversationId,
                            data: checklistData,
                          })
                        }
                        className="px-3 py-1.5 rounded-lg border border-outline-variant text-xs font-semibold text-on-surface hover:bg-surface-container-low cursor-pointer transition-colors"
                      >
                        Xem toàn bộ lộ trình ({checklistData.totalCount} việc)
                      </button>
                    )}
                  </div>

                  {/* Direct Chat with Mother */}
                  <button
                    type="button"
                    onClick={() => navigate(`/direct-chats/${card.conversationId}`)}
                    className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-primary text-white text-xs font-semibold hover:bg-primary/90 transition-colors shadow-xs cursor-pointer ml-auto"
                  >
                    <span className="material-symbols-outlined text-base">chat</span>
                    Nhắn tin tư vấn
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* --- MODAL 1: CHI TIẾT LỊCH SỬ CHỈ SỐ SỨC KHỎE --- */}
      {selectedHealthModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-surface rounded-2xl border border-outline-variant shadow-xl max-w-2xl w-full max-h-[90vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            {/* Modal Header */}
            <div className="p-6 border-b border-outline-variant/50 flex items-center justify-between bg-surface-container-low/60">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-primary text-white flex items-center justify-center shadow-xs">
                  <span className="material-symbols-outlined text-xl">vital_signs</span>
                </div>
                <div>
                  <h3 className="text-base font-bold text-on-surface m-0">
                    Lịch sử chỉ số: {selectedHealthModal.motherName}
                  </h3>
                  <p className="text-xs text-on-surface-variant m-0 mt-0.5">
                    {selectedHealthModal.data.gestationalWeek
                      ? `Tuần thai ${selectedHealthModal.data.gestationalWeek} · `
                      : ''}
                    {selectedHealthModal.data.timeRangeLabel || 'Toàn bộ khoảng thời gian'}
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setSelectedHealthModal(null)}
                className="w-8 h-8 rounded-full hover:bg-surface-container-high flex items-center justify-center text-on-surface-variant cursor-pointer transition-colors"
              >
                <span className="material-symbols-outlined text-xl">close</span>
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 overflow-y-auto space-y-4">
              <div className="space-y-3">
                {selectedHealthModal.data.metrics.map((metric) => (
                  <div
                    key={metric.code}
                    className="p-4 rounded-xl border border-outline-variant/60 bg-surface-container-lowest space-y-3"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="material-symbols-outlined text-primary text-lg">
                          {getMetricIcon(metric.code)}
                        </span>
                        <span className="text-sm font-bold text-on-surface">{metric.name}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-bold text-on-surface">
                          {metric.value} {metric.unit}
                        </span>
                        {getStatusBadge(metric.status)}
                      </div>
                    </div>

                    {/* Timeline History Points for this metric */}
                    {metric.history && metric.history.length > 0 && (
                      <div className="border-t border-outline-variant/40 pt-2.5 mt-2">
                        <p className="text-[11px] font-bold text-on-surface-variant mb-2">
                          Lịch sử các lần đo gần đây ({metric.history.length} lần):
                        </p>
                        <div className="space-y-1.5 max-h-36 overflow-y-auto pr-1">
                          {metric.history.map((h, hIdx) => (
                            <div
                              key={hIdx}
                              className="flex items-center justify-between text-xs p-2 rounded-lg bg-surface-container-low/50"
                            >
                              <span className="text-on-surface-variant text-[11px]">{h.measuredAt}</span>
                              <div className="flex items-center gap-2">
                                <span className="font-semibold text-on-surface">
                                  {h.value} {h.unit}
                                </span>
                                {getStatusBadge(h.status)}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>

              {selectedHealthModal.data.note && (
                <div className="p-3.5 rounded-xl bg-surface-container-low/80 border border-outline-variant/50">
                  <p className="text-xs font-bold text-on-surface mb-1">Ghi chú từ mẹ bầu:</p>
                  <p className="text-xs italic text-on-surface-variant m-0">{selectedHealthModal.data.note}</p>
                </div>
              )}
            </div>

            {/* Modal Footer */}
            <div className="p-4 border-t border-outline-variant/50 bg-surface flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => setSelectedHealthModal(null)}
                className="px-4 py-2 rounded-lg border border-outline-variant text-xs font-semibold text-on-surface hover:bg-surface-container-low cursor-pointer"
              >
                Đóng
              </button>
              <button
                type="button"
                onClick={() => {
                  const convId = selectedHealthModal.conversationId;
                  setSelectedHealthModal(null);
                  navigate(`/direct-chats/${convId}`);
                }}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-primary text-white text-xs font-semibold hover:bg-primary/90 shadow-xs cursor-pointer"
              >
                <span className="material-symbols-outlined text-base">chat</span>
                Nhắn tin tư vấn cho mẹ
              </button>
            </div>
          </div>
        </div>
      )}

      {/* --- MODAL 2: CHI TIẾT LỘ TRÌNH CHECKLIST & CÁ NHÂN HÓA BỞI BÁC SĨ --- */}
      {selectedChecklistModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-surface rounded-2xl border border-outline-variant shadow-xl max-w-2xl w-full max-h-[90vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            {/* Modal Header */}
            <div className="p-6 border-b border-outline-variant/50 flex items-center justify-between bg-surface-container-low/60">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-primary text-white flex items-center justify-center shadow-xs">
                  <span className="material-symbols-outlined text-xl">assignment_turned_in</span>
                </div>
                <div>
                  <h3 className="text-base font-bold text-on-surface m-0">
                    Hồ sơ Checklist: {selectedChecklistModal.motherName}
                  </h3>
                  <p className="text-xs text-on-surface-variant m-0 mt-0.5">
                    {selectedChecklistModal.data.gestationalWeek
                      ? `Tuần thai ${selectedChecklistModal.data.gestationalWeek} · `
                      : ''}
                    Tiến độ: {selectedChecklistModal.data.completedCount}/{selectedChecklistModal.data.totalCount} việc (
                    {selectedChecklistModal.data.progressPercent}%)
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() =>
                    setTaskFormModal({
                      isOpen: true,
                      mode: 'ADD',
                      targetGroup: checklistModalTab,
                      text: '',
                      category: 'Khám thai & Y tế',
                      completed: false,
                      doctorNote: '',
                      conversationId: selectedChecklistModal.conversationId,
                      checklistData: selectedChecklistModal.data,
                      motherName: selectedChecklistModal.motherName,
                    })
                  }
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary text-white text-xs font-semibold hover:bg-primary/90 shadow-xs transition-colors cursor-pointer"
                >
                  <span className="material-symbols-outlined text-sm">add</span>
                  Thêm việc cho mẹ
                </button>

                <button
                  type="button"
                  onClick={() => setSelectedChecklistModal(null)}
                  className="w-8 h-8 rounded-full hover:bg-surface-container-high flex items-center justify-center text-on-surface-variant cursor-pointer transition-colors"
                >
                  <span className="material-symbols-outlined text-xl">close</span>
                </button>
              </div>
            </div>

            {/* Modal Tabs */}
            <div className="flex border-b border-outline-variant/40 bg-surface-container-low/50 px-6 pt-3 gap-2">
              {(() => {
                const historyTexts = new Set(
                  (selectedChecklistModal.data.historyItems || []).map((h) => h.text.trim().toLowerCase())
                );
                const currentFiltered = (
                  selectedChecklistModal.data.currentItems ||
                  selectedChecklistModal.data.items ||
                  []
                ).filter((item) => !historyTexts.has(item.text.trim().toLowerCase()));

                return (
                  <>
                    <button
                      type="button"
                      onClick={() => setChecklistModalTab('CURRENT')}
                      className={`pb-2.5 px-3 text-xs font-bold border-b-2 transition-colors cursor-pointer ${
                        checklistModalTab === 'CURRENT'
                          ? 'border-primary text-primary'
                          : 'border-transparent text-on-surface-variant hover:text-on-surface'
                      }`}
                    >
                      Tuần hiện tại ({currentFiltered.length})
                    </button>
                    <button
                      type="button"
                      onClick={() => setChecklistModalTab('HISTORY')}
                      className={`pb-2.5 px-3 text-xs font-bold border-b-2 transition-colors cursor-pointer ${
                        checklistModalTab === 'HISTORY'
                          ? 'border-primary text-primary'
                          : 'border-transparent text-on-surface-variant hover:text-on-surface'
                      }`}
                    >
                      Lịch sử đã qua ({selectedChecklistModal.data.historyItems?.length || 0})
                    </button>
                    <button
                      type="button"
                      onClick={() => setChecklistModalTab('FUTURE')}
                      className={`pb-2.5 px-3 text-xs font-bold border-b-2 transition-colors cursor-pointer ${
                        checklistModalTab === 'FUTURE'
                          ? 'border-primary text-primary'
                          : 'border-transparent text-on-surface-variant hover:text-on-surface'
                      }`}
                    >
                      Lộ trình tương lai ({selectedChecklistModal.data.futureItems?.length || 0})
                    </button>
                  </>
                );
              })()}
            </div>

            {/* Modal Origin Sub-Filter */}
            <div className="flex items-center gap-2 px-6 py-2.5 border-b border-outline-variant/30 bg-surface flex-wrap">
              <span className="text-[11px] font-semibold text-on-surface-variant">Phân loại nguồn việc:</span>
              <button
                type="button"
                onClick={() => setChecklistModalOriginFilter('ALL')}
                className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                  checklistModalOriginFilter === 'ALL'
                    ? 'bg-primary text-white shadow-xs'
                    : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container-high'
                }`}
              >
                Tất cả
              </button>
              <button
                type="button"
                onClick={() => setChecklistModalOriginFilter('CAREBRIDGE')}
                className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                  checklistModalOriginFilter === 'CAREBRIDGE'
                    ? 'bg-sky-700 text-white shadow-xs'
                    : 'bg-sky-50 text-sky-800 border border-sky-200 hover:bg-sky-100'
                }`}
              >
                ✨ Gợi ý CareBridge
              </button>
              <button
                type="button"
                onClick={() => setChecklistModalOriginFilter('PERSONAL')}
                className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                  checklistModalOriginFilter === 'PERSONAL'
                    ? 'bg-purple-700 text-white shadow-xs'
                    : 'bg-purple-50 text-purple-800 border border-purple-200 hover:bg-purple-100'
                }`}
              >
                👤 Việc cá nhân
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 overflow-y-auto space-y-4">
              {/* CURRENT TAB */}
              {checklistModalTab === 'CURRENT' && (
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <p className="text-xs font-semibold text-on-surface-variant m-0">
                      Các việc cần làm trong tuần thai / giai đoạn hiện tại:
                    </p>
                    <span className="text-[11px] text-primary font-medium">Bác sĩ có thể chỉnh sửa hoặc xóa để cá nhân hóa</span>
                  </div>

                  {(() => {
                    const historyTexts = new Set(
                      (selectedChecklistModal.data.historyItems || []).map((h) => h.text.trim().toLowerCase())
                    );
                    const currentFiltered = (
                      selectedChecklistModal.data.currentItems ||
                      selectedChecklistModal.data.items ||
                      []
                    ).filter((item) => !historyTexts.has(item.text.trim().toLowerCase()));

                    const displayed = currentFiltered.filter((item) => {
                      const originCat = getTaskOriginCategory(item);
                      const isCareBridge = originCat === 'CAREBRIDGE';
                      if (checklistModalOriginFilter === 'CAREBRIDGE') return isCareBridge;
                      if (checklistModalOriginFilter === 'PERSONAL') return !isCareBridge;
                      return true;
                    });

                    if (displayed.length === 0) {
                      return (
                        <div className="p-8 text-center text-xs text-outline italic bg-surface-container-low/40 rounded-xl">
                          Không có công việc nào phù hợp trong tuần này.
                        </div>
                      );
                    }

                    return (
                      <div className="space-y-2">
                        {displayed.map((item, idx) => {
                          const originCat = getTaskOriginCategory(item);
                          return (
                          <div
                            key={idx}
                            className={`p-3 rounded-xl border flex items-start justify-between gap-3 text-xs transition-all ${
                              item.completed
                                ? 'bg-emerald-50/50 border-emerald-200/60 text-emerald-950'
                                : 'bg-surface border-outline-variant/50 text-on-surface hover:border-primary/40'
                            }`}
                          >
                            <div
                              onClick={() =>
                                handleToggleTaskStatus(
                                  selectedChecklistModal.conversationId,
                                  selectedChecklistModal.data,
                                  'CURRENT',
                                  idx,
                                  item
                                )
                              }
                              className="flex items-start gap-2.5 min-w-0 cursor-pointer flex-1"
                            >
                              <span
                                className={`material-symbols-outlined text-base shrink-0 mt-0.5 transition-transform active:scale-90 ${
                                  item.completed ? 'text-emerald-600' : 'text-outline'
                                }`}
                              >
                                {item.completed ? 'check_circle' : 'radio_button_unchecked'}
                              </span>
                              <div className="min-w-0 flex-1">
                                <div className="flex items-center gap-1.5 flex-wrap">
                                  <span className={item.completed ? 'font-medium' : 'text-on-surface font-semibold'}>
                                    {item.text}
                                  </span>
                                  {originCat === 'USER' ? (
                                    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold bg-purple-50 text-purple-800 border border-purple-200">
                                      <span className="material-symbols-outlined text-xs text-purple-600">person</span>
                                      Việc cá nhân
                                    </span>
                                  ) : (
                                    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold bg-sky-50 text-sky-800 border border-sky-200">
                                      <span className="material-symbols-outlined text-xs text-sky-600">auto_awesome</span>
                                      Gợi ý CareBridge
                                    </span>
                                  )}
                                </div>
                              <div className="flex flex-wrap gap-2 text-[10px] text-on-surface-variant mt-1">
                                {item.timeLabel && (
                                  <span className="px-1.5 py-0.2 bg-surface-container-high rounded font-medium">
                                    {item.timeLabel}
                                  </span>
                                )}
                                {item.category && (
                                  <span className="px-1.5 py-0.2 bg-primary/10 text-primary rounded font-medium">
                                    {item.category}
                                  </span>
                                )}
                              </div>
                              {item.doctorNote && (
                                <p className="text-[11px] text-teal-700 italic mt-1 m-0 flex items-center gap-1">
                                  <span className="material-symbols-outlined text-xs">chat</span>
                                  Lời dặn: {item.doctorNote}
                                </p>
                              )}
                              {item.sourceUrl && (
                                <a
                                  href={item.sourceUrl}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  onClick={(e) => e.stopPropagation()}
                                  className="inline-flex items-center gap-1 text-[10px] font-semibold text-primary hover:underline mt-1"
                                >
                                  <span className="material-symbols-outlined text-xs">link</span>
                                  Nguồn tham khảo
                                </a>
                              )}
                            </div>
                          </div>

                          <div className="flex items-center gap-2 shrink-0">
                            <span
                              className={`px-2 py-0.5 text-[10px] font-bold rounded ${
                                item.completed ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                              }`}
                            >
                              {item.completed ? 'Đã xong' : 'Chờ làm'}
                            </span>

                            {/* Edit Button */}
                            <button
                              type="button"
                              title="Sửa việc cần làm"
                              onClick={() =>
                                setTaskFormModal({
                                  isOpen: true,
                                  mode: 'EDIT',
                                  targetGroup: 'CURRENT',
                                  itemIndex: idx,
                                  text: item.text,
                                  category: item.category || 'Khám thai & Y tế',
                                  timeLabel: item.timeLabel,
                                  completed: item.completed,
                                  doctorNote: item.doctorNote || '',
                                  sourceUrl: item.sourceUrl || '',
                                  conversationId: selectedChecklistModal.conversationId,
                                  checklistData: selectedChecklistModal.data,
                                  motherName: selectedChecklistModal.motherName,
                                })
                              }
                              className="w-7 h-7 rounded-lg flex items-center justify-center text-on-surface-variant hover:text-primary hover:bg-primary/10 transition-colors cursor-pointer"
                            >
                              <span className="material-symbols-outlined text-base">edit</span>
                            </button>

                            {/* Delete Button */}
                            <button
                              type="button"
                              title="Xóa việc cần làm"
                              onClick={() =>
                                setDeleteConfirmModal({
                                  isOpen: true,
                                  targetGroup: 'CURRENT',
                                  itemIndex: idx,
                                  itemText: item.text,
                                  conversationId: selectedChecklistModal.conversationId,
                                  checklistData: selectedChecklistModal.data,
                                })
                              }
                              className="w-7 h-7 rounded-lg flex items-center justify-center text-on-surface-variant hover:text-rose-600 hover:bg-rose-100 transition-colors cursor-pointer"
                            >
                              <span className="material-symbols-outlined text-base">delete</span>
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                );
              })()}
              </div>
            )}

              {/* HISTORY TAB */}
              {checklistModalTab === 'HISTORY' && (
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <p className="text-xs font-semibold text-on-surface-variant m-0">
                      Các nhiệm vụ và mốc khám thai đã hoàn thành trong các giai đoạn trước:
                    </p>
                  </div>

                  {(() => {
                    const displayed = (selectedChecklistModal.data.historyItems || []).filter((item) => {
                      const originCat = getTaskOriginCategory(item);
                      const isCareBridge = originCat === 'CAREBRIDGE';
                      if (checklistModalOriginFilter === 'CAREBRIDGE') return isCareBridge;
                      if (checklistModalOriginFilter === 'PERSONAL') return !isCareBridge;
                      return true;
                    });

                    if (displayed.length === 0) {
                      return (
                        <div className="p-8 text-center text-xs text-outline italic bg-surface-container-low/40 rounded-xl">
                          Chưa có lịch sử nhiệm vụ nào phù hợp với bộ lọc.
                        </div>
                      );
                    }

                    return (
                      <div className="space-y-2">
                        {displayed.map((item, idx) => {
                          const originCat = getTaskOriginCategory(item);
                          return (
                          <div
                            key={idx}
                            className="p-3 rounded-xl border border-emerald-200/60 bg-emerald-50/50 flex items-start justify-between gap-3 text-xs"
                          >
                            <div
                              onClick={() =>
                                handleToggleTaskStatus(
                                  selectedChecklistModal.conversationId,
                                  selectedChecklistModal.data,
                                  'HISTORY',
                                  idx,
                                  item
                                )
                              }
                              className="flex items-start gap-2.5 min-w-0 cursor-pointer flex-1"
                            >
                              <span className="material-symbols-outlined text-emerald-600 text-base shrink-0 mt-0.5">
                                {item.completed ? 'check_circle' : 'radio_button_unchecked'}
                              </span>
                              <div className="min-w-0 flex-1">
                                <div className="flex items-center gap-1.5 flex-wrap">
                                  <span className="font-medium text-emerald-950">{item.text}</span>
                                  {originCat === 'USER' ? (
                                    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold bg-purple-50 text-purple-800 border border-purple-200">
                                      <span className="material-symbols-outlined text-xs text-purple-600">person</span>
                                      Việc cá nhân
                                    </span>
                                  ) : (
                                    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold bg-sky-50 text-sky-800 border border-sky-200">
                                      <span className="material-symbols-outlined text-xs text-sky-600">auto_awesome</span>
                                      Gợi ý CareBridge
                                    </span>
                                  )}
                                </div>
                                <div className="flex flex-wrap gap-2 text-[10px] text-emerald-800/80 mt-1">
                                  {item.timeLabel && <span>{item.timeLabel}</span>}
                                  {item.category && <span>· {item.category}</span>}
                                </div>
                                {item.doctorNote && (
                                  <p className="text-[11px] text-teal-700 italic mt-1 m-0 flex items-center gap-1">
                                    <span className="material-symbols-outlined text-xs">chat</span>
                                    Lời dặn: {item.doctorNote}
                                  </p>
                                )}
                                {item.sourceUrl && (
                                  <a
                                    href={item.sourceUrl}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    onClick={(e) => e.stopPropagation()}
                                    className="inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-800 hover:underline mt-1"
                                  >
                                    <span className="material-symbols-outlined text-xs">link</span>
                                    Nguồn tham khảo
                                  </a>
                                )}
                              </div>
                            </div>

                            <div className="flex items-center gap-2 shrink-0">
                              <span className="px-2 py-0.5 text-[10px] font-bold rounded bg-emerald-100 text-emerald-800">
                                {item.completed ? 'Đã xong' : 'Chưa xong'}
                              </span>

                              <button
                                type="button"
                                title="Sửa việc cần làm"
                                onClick={() =>
                                  setTaskFormModal({
                                    isOpen: true,
                                    mode: 'EDIT',
                                    targetGroup: 'HISTORY',
                                    itemIndex: idx,
                                    text: item.text,
                                    category: item.category || 'Khám thai & Y tế',
                                    timeLabel: item.timeLabel,
                                    completed: item.completed,
                                    doctorNote: item.doctorNote || '',
                                    sourceUrl: item.sourceUrl || '',
                                    conversationId: selectedChecklistModal.conversationId,
                                    checklistData: selectedChecklistModal.data,
                                    motherName: selectedChecklistModal.motherName,
                                  })
                                }
                                className="w-7 h-7 rounded-lg flex items-center justify-center text-emerald-800 hover:bg-emerald-200/50 transition-colors cursor-pointer"
                              >
                                <span className="material-symbols-outlined text-base">edit</span>
                              </button>

                              <button
                                type="button"
                                title="Xóa việc cần làm"
                                onClick={() =>
                                  setDeleteConfirmModal({
                                    isOpen: true,
                                    targetGroup: 'HISTORY',
                                    itemIndex: idx,
                                    itemText: item.text,
                                    conversationId: selectedChecklistModal.conversationId,
                                    checklistData: selectedChecklistModal.data,
                                  })
                                }
                                className="w-7 h-7 rounded-lg flex items-center justify-center text-rose-600 hover:bg-rose-100 transition-colors cursor-pointer"
                              >
                                <span className="material-symbols-outlined text-base">delete</span>
                              </button>
                            </div>
                          </div>
                        );
                      })}
                      </div>
                    );
                  })()}
                </div>
              )}

              {/* FUTURE TAB */}
              {checklistModalTab === 'FUTURE' && (
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <p className="text-xs font-semibold text-on-surface-variant m-0">
                      Lộ trình chuẩn bị y tế và chăm sóc dự kiến trong các tuần tương lai:
                    </p>
                  </div>

                  {(() => {
                    const displayed = (selectedChecklistModal.data.futureItems || []).filter((item) => {
                      const originCat = getTaskOriginCategory(item);
                      const isCareBridge = originCat === 'CAREBRIDGE';
                      if (checklistModalOriginFilter === 'CAREBRIDGE') return isCareBridge;
                      if (checklistModalOriginFilter === 'PERSONAL') return !isCareBridge;
                      return true;
                    });

                    if (displayed.length === 0) {
                      return (
                        <div className="p-8 text-center text-xs text-outline italic bg-surface-container-low/40 rounded-xl">
                          Chưa có lộ trình tương lai nào phù hợp với bộ lọc.
                        </div>
                      );
                    }

                    return (
                      <div className="space-y-2">
                        {displayed.map((item, idx) => {
                          const originCat = getTaskOriginCategory(item);
                          return (
                          <div
                            key={idx}
                            className="p-3 rounded-xl border border-purple-200/60 bg-purple-50/40 flex items-start justify-between gap-3 text-xs"
                          >
                            <div
                              onClick={() =>
                                handleToggleTaskStatus(
                                  selectedChecklistModal.conversationId,
                                  selectedChecklistModal.data,
                                  'FUTURE',
                                  idx,
                                  item
                                )
                              }
                              className="flex items-start gap-2.5 min-w-0 cursor-pointer flex-1"
                            >
                              <span className="material-symbols-outlined text-purple-600 text-base shrink-0 mt-0.5">
                                {item.completed ? 'check_circle' : 'upcoming'}
                              </span>
                              <div className="min-w-0 flex-1">
                                <div className="flex items-center gap-1.5 flex-wrap">
                                  <span className="font-medium text-purple-950">{item.text}</span>
                                  {originCat === 'USER' ? (
                                    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold bg-purple-50 text-purple-800 border border-purple-200">
                                      <span className="material-symbols-outlined text-xs text-purple-600">person</span>
                                      Việc cá nhân
                                    </span>
                                  ) : (
                                    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold bg-sky-50 text-sky-800 border border-sky-200">
                                      <span className="material-symbols-outlined text-xs text-sky-600">auto_awesome</span>
                                      Gợi ý CareBridge
                                    </span>
                                  )}
                                </div>
                                <div className="flex flex-wrap gap-2 text-[10px] text-purple-800/80 mt-1">
                                  {item.timeLabel && <span>{item.timeLabel}</span>}
                                  {item.category && <span>· {item.category}</span>}
                                </div>
                                {item.doctorNote && (
                                  <p className="text-[11px] text-teal-700 italic mt-1 m-0 flex items-center gap-1">
                                    <span className="material-symbols-outlined text-xs">chat</span>
                                    Lời dặn: {item.doctorNote}
                                  </p>
                                )}
                                {item.sourceUrl && (
                                  <a
                                    href={item.sourceUrl}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    onClick={(e) => e.stopPropagation()}
                                    className="inline-flex items-center gap-1 text-[10px] font-semibold text-purple-800 hover:underline mt-1"
                                  >
                                    <span className="material-symbols-outlined text-xs">link</span>
                                    Nguồn tham khảo
                                  </a>
                                )}
                              </div>
                            </div>

                            <div className="flex items-center gap-2 shrink-0">
                              <span className="px-2 py-0.5 text-[10px] font-bold rounded bg-purple-100 text-purple-800">
                                Chờ đến tuần
                              </span>

                              <button
                                type="button"
                                title="Sửa việc cần làm"
                                onClick={() =>
                                  setTaskFormModal({
                                    isOpen: true,
                                    mode: 'EDIT',
                                    targetGroup: 'FUTURE',
                                    itemIndex: idx,
                                    text: item.text,
                                    category: item.category || 'Khám thai & Y tế',
                                    timeLabel: item.timeLabel,
                                    completed: item.completed,
                                    doctorNote: item.doctorNote || '',
                                    sourceUrl: item.sourceUrl || '',
                                    conversationId: selectedChecklistModal.conversationId,
                                    checklistData: selectedChecklistModal.data,
                                    motherName: selectedChecklistModal.motherName,
                                  })
                                }
                                className="w-7 h-7 rounded-lg flex items-center justify-center text-purple-800 hover:bg-purple-200/50 transition-colors cursor-pointer"
                              >
                                <span className="material-symbols-outlined text-base">edit</span>
                              </button>

                              <button
                                type="button"
                                title="Xóa việc cần làm"
                                onClick={() =>
                                  setDeleteConfirmModal({
                                    isOpen: true,
                                    targetGroup: 'FUTURE',
                                    itemIndex: idx,
                                    itemText: item.text,
                                    conversationId: selectedChecklistModal.conversationId,
                                    checklistData: selectedChecklistModal.data,
                                  })
                                }
                                className="w-7 h-7 rounded-lg flex items-center justify-center text-rose-600 hover:bg-rose-100 transition-colors cursor-pointer"
                              >
                                <span className="material-symbols-outlined text-base">delete</span>
                              </button>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  );
                })()}
                </div>
              )}

              {selectedChecklistModal.data.note && (
                <div className="p-3.5 rounded-xl bg-surface-container-low/80 border border-outline-variant/50">
                  <p className="text-xs font-bold text-on-surface mb-1">Ghi chú / Chỉ định:</p>
                  <p className="text-xs italic text-on-surface-variant m-0">{selectedChecklistModal.data.note}</p>
                </div>
              )}
            </div>

            {/* Modal Footer */}
            <div className="p-4 border-t border-outline-variant/50 bg-surface flex items-center justify-between gap-3">
              <span className="text-xs text-on-surface-variant">
                Đã cá nhân hóa: {selectedChecklistModal.data.totalCount} việc
              </span>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setSelectedChecklistModal(null)}
                  className="px-4 py-2 rounded-lg border border-outline-variant text-xs font-semibold text-on-surface hover:bg-surface-container-low cursor-pointer"
                >
                  Đóng
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const convId = selectedChecklistModal.conversationId;
                    setSelectedChecklistModal(null);
                    navigate(`/direct-chats/${convId}`);
                  }}
                  className="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-primary text-white text-xs font-semibold hover:bg-primary/90 shadow-xs cursor-pointer"
                >
                  <span className="material-symbols-outlined text-base">chat</span>
                  Nhắn tin tư vấn cho mẹ
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* --- MODAL 3: FORM THÊM / SỬA CHECKLIST CHUẨN CONTENT ADMIN (EXPERT CUSTOMIZATION) --- */}
      {taskFormModal && (
        <ExpertChecklistFormModal
          isOpen={taskFormModal.isOpen}
          onClose={() => setTaskFormModal(null)}
          onSuccess={handleTaskSavedFromModal}
          mode={taskFormModal.mode}
          motherName={taskFormModal.motherName}
          conversationId={taskFormModal.conversationId}
          checklistData={taskFormModal.checklistData}
          initialItemIndex={taskFormModal.itemIndex}
          initialTargetGroup={taskFormModal.targetGroup}
          initialItem={
            taskFormModal.mode === 'EDIT'
              ? {
                  text: taskFormModal.text,
                  completed: taskFormModal.completed,
                  category: taskFormModal.category,
                  timeLabel: taskFormModal.timeLabel,
                  doctorNote: taskFormModal.doctorNote,
                  sourceUrl: taskFormModal.sourceUrl,
                }
              : undefined
          }
        />
      )}

      {/* --- MODAL 4: XÁC NHẬN XÓA VIỆC CẦN LÀM --- */}
      {deleteConfirmModal && (
        <div
          className="fixed inset-0 z-[90] bg-black/50 backdrop-blur-xs flex items-center justify-center p-4"
          style={{ zIndex: 90 }}
        >
          <div className="bg-surface rounded-2xl border border-outline-variant shadow-2xl max-w-md w-full overflow-hidden animate-in fade-in zoom-in-95 duration-200 p-6 space-y-4">
            <div className="flex items-center gap-3 text-rose-600">
              <div className="w-10 h-10 rounded-xl bg-rose-100 flex items-center justify-center">
                <span className="material-symbols-outlined text-xl">delete_forever</span>
              </div>
              <div>
                <h4 className="text-sm font-bold text-on-surface m-0">Xóa việc cần làm?</h4>
                <p className="text-xs text-on-surface-variant m-0 mt-0.5">Thao tác này sẽ xóa mục khỏi lộ trình của mẹ bầu.</p>
              </div>
            </div>

            <div className="p-3 bg-surface-container-low rounded-xl border border-outline-variant/40 text-xs font-medium text-on-surface">
              &quot;{deleteConfirmModal.itemText}&quot;
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-2">
              <button
                type="button"
                onClick={() => setDeleteConfirmModal(null)}
                className="px-4 py-2 rounded-xl border border-outline-variant text-xs font-semibold text-on-surface hover:bg-surface-container-low cursor-pointer transition-colors"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleDeleteTask}
                disabled={savingTask}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-rose-600 text-white text-xs font-bold hover:bg-rose-700 disabled:opacity-50 transition-colors shadow-xs cursor-pointer"
              >
                {savingTask ? 'Đang xóa...' : 'Xác nhận xóa'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* --- TOAST NOTIFICATION --- */}
      {toastMessage && (
        <div
          className="fixed bottom-6 right-6 z-[100] flex items-center gap-2.5 px-4 py-3 rounded-xl bg-on-surface text-surface shadow-xl text-xs font-semibold animate-in slide-in-from-bottom-5 fade-in duration-200"
          style={{ zIndex: 100 }}
        >
          <span
            className={`material-symbols-outlined text-base ${
              toastMessage.type === 'success' ? 'text-emerald-400' : 'text-rose-400'
            }`}
          >
            {toastMessage.type === 'success' ? 'check_circle' : 'error'}
          </span>
          {toastMessage.text}
        </div>
      )}
    </div>
  );
}

