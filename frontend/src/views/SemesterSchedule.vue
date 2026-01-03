<template>
  <div class="semester-schedule">
    <el-card class="schedule-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <div class="title-wrap">
              <span class="title">学期课程表</span>
              <span class="subtitle">按班级/教师查看周课表</span>
            </div>
          </div>

          <div class="header-actions">
            <el-space :size="10" wrap>
              <!-- 学期选择 -->
              <el-select
                  v-model="selectedSemesterId"
                  placeholder="选择学期"
                  style="width: 220px"
                  :loading="loadingSemesters"
                  clearable
              >
                <el-option
                    v-for="semester in semesters"
                    :key="semester.semesterId"
                    :label="semester.semesterName"
                    :value="semester.semesterId"
                />
              </el-select>

              <!-- 视角选择 -->
              <el-radio-group v-model="viewMode" size="small" class="mode-group">
                <el-radio-button :label="'class'">班级视角</el-radio-button>
                <el-radio-button :label="'teacher'">教师视角</el-radio-button>
              </el-radio-group>

              <!-- 班级选择 -->
              <el-select
                  v-if="viewMode === 'class'"
                  v-model="selectedClassName"
                  placeholder="选择班级"
                  style="width: 180px"
                  :loading="loadingOptions"
                  clearable
              >
                <el-option
                    v-for="className in classNames"
                    :key="className"
                    :label="className"
                    :value="className"
                />
              </el-select>

              <!-- 教师选择 -->
              <el-select
                  v-else
                  v-model="selectedTeacher"
                  placeholder="选择教师"
                  style="width: 180px"
                  :loading="loadingOptions"
                  clearable
              >
                <el-option
                    v-for="teacher in teacherNames"
                    :key="teacher"
                    :label="teacher"
                    :value="teacher"
                />
              </el-select>

              <!-- 查询 -->
              <el-button
                  type="primary"
                  :loading="loadingSchedule"
                  :disabled="!canQuery"
                  @click="fetchWeekSchedule"
              >
                <el-icon><Search /></el-icon>
                查询
              </el-button>

              <!-- 导出 -->
              <el-button type="success" :disabled="!canExport" @click="handleExport">
                <el-icon><Download /></el-icon>
                导出课表
              </el-button>
            </el-space>
          </div>
        </div>
      </template>

      <!-- 周课表展示 -->
      <div class="week-schedule-container">
        <!-- 周标题 -->
        <div class="week-header">
          <div class="week-head-empty"></div>
          <div class="week-day" v-for="day in weekDays" :key="day.value">
            <div class="day-label">{{ day.label }}</div>
          </div>
        </div>

        <!-- 内容区：时间轴 + 网格 -->
        <div class="schedule-content">
          <!-- 左侧时间轴 -->
          <div class="time-slot">
            <div class="slot-time" v-for="slot in timeSlots" :key="slot.section">
              {{ slot.time }}
            </div>
          </div>

          <!-- 右侧课程网格 -->
          <div
              class="course-grid"
              :style="{
              '--rows': timeSlots.length,
              '--cols': weekDays.length,
            }"
              v-loading="loadingSchedule"
          >
            <!-- 背景格子（网格感） -->
            <div
                class="grid-bg-cell"
                v-for="bg in gridBgCells"
                :key="bg.key"
                :style="{ gridColumn: bg.col, gridRow: bg.row }"
            />

            <!-- 课程格子 -->
            <div
                class="course-cell"
                v-for="course in weekSchedule"
                :key="course.scheduleId"
                :style="getCourseCellStyle(course)"
                @click="handleClickCourse(course)"
                role="button"
                tabindex="0"
            >
              <div class="course-info">
                <div class="course-name" :title="course.courseName || '未命名课程'">
                  {{ course.courseName || "未命名课程" }}
                </div>
                <div class="course-meta">
                  <span class="course-teacher" :title="course.teacher || ''">
                    {{ course.teacher || "-" }}
                  </span>
                  <span class="dot">·</span>
                  <span class="course-classroom" :title="course.classroom || ''">
                    {{ course.classroom || "-" }}
                  </span>
                </div>
              </div>
              <div class="course-hover-hint">点击查看</div>
            </div>

            <!-- 空状态 -->
            <div v-if="!loadingSchedule && weekSchedule.length === 0" class="empty-tip">
              请选择学期与班级/教师后点击“查询”
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from "vue";
import { ElMessage } from "element-plus";
import { Search, Download } from "@element-plus/icons-vue";
import {
  getCurrentSemester,
  getAllSemesters,
  getWeekScheduleByClass,
  getWeekScheduleByTeacher,
  getCourseSchedules,
} from "../api/semester";

// 周几选项
const weekDays = [
  { value: 1, label: "周一" },
  { value: 2, label: "周二" },
  { value: 3, label: "周三" },
  { value: 4, label: "周四" },
  { value: 5, label: "周五" },
  { value: 6, label: "周六" },
  { value: 7, label: "周日" },
];

// 时间段选项（你这里是 5 段）
const timeSlots = [
  { section: 1, time: "08:20-10:00" },
  { section: 2, time: "10:20-12:00" },
  { section: 3, time: "14:00-15:40" },
  { section: 4, time: "16:00-17:40" },
  { section: 5, time: "18:30-20:10" },
];

// 学期 & 视角
const semesters = ref([]);
const selectedSemesterId = ref(null);

const viewMode = ref("class"); // class | teacher
const selectedClassName = ref("");
const selectedTeacher = ref("");

// 下拉列表
const classNames = ref([]);
const teacherNames = ref([]);

// 周课表数据
const weekSchedule = ref([]);

// loading
const loadingSemesters = ref(false);
const loadingOptions = ref(false);
const loadingSchedule = ref(false);

// 是否可查询/导出
const canQuery = computed(() => {
  if (!selectedSemesterId.value) return false;
  if (viewMode.value === "class") return !!selectedClassName.value;
  return !!selectedTeacher.value;
});
const canExport = computed(() => canQuery.value);

// ========== 背景格子生成（rows * cols） ==========
const gridBgCells = computed(() => {
  const cells = [];
  for (let r = 1; r <= timeSlots.length; r++) {
    for (let c = 1; c <= weekDays.length; c++) {
      cells.push({ key: `${r}-${c}`, row: r, col: c });
    }
  }
  return cells;
});

// ========== 稳定上色（不随机、不跳） ==========
const colorPalette = [
  "#FFE4E1",
  "#E6F3FF",
  "#E8F5E9",
  "#FFF3E0",
  "#F3E5F5",
  "#E0F7FA",
  "#FFF8E1",
  "#FCE4EC",
  "#E8F5E8",
  "#E3F2FD",
];

const hashCode = (str) => {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = (hash << 5) - hash + str.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
};

const getStableColor = (course) => {
  const key = `${course.courseName || ""}|${course.teacher || ""}|${course.classroom || ""}`;
  const idx = hashCode(key) % colorPalette.length;
  return colorPalette[idx];
};

// 课程格子样式
const getCourseCellStyle = (course) => {
  return {
    gridColumn: course.weekDay,      // 1~7
    gridRow: course.classSection,    // 1~timeSlots.length（你的后端需匹配）
    backgroundColor: getStableColor(course),
  };
};

// 点击课程
const handleClickCourse = (course) => {
  ElMessage.info(
      `${course.courseName || "未命名课程"} / ${course.teacher || "-"} / ${course.classroom || "-"}`
  );
};

// ========== 获取学期 ==========
const fetchSemesters = async () => {
  loadingSemesters.value = true;
  try {
    const [all, current] = await Promise.all([
      getAllSemesters(),
      getCurrentSemester(),
    ]);
    semesters.value = all || [];

    if (current?.semesterId) {
      selectedSemesterId.value = current.semesterId;
    } else if (semesters.value.length > 0) {
      selectedSemesterId.value = semesters.value[0].semesterId;
    }
  } catch (error) {
    ElMessage.error("获取学期列表失败");
    console.error("获取学期列表失败:", error);
  } finally {
    loadingSemesters.value = false;
  }
};

// ========== 获取班级/教师（一次请求） ==========
const fetchOptionsBySemester = async (semesterId) => {
  if (!semesterId) return;

  loadingOptions.value = true;
  try {
    const list = await getCourseSchedules(semesterId);

    const classes = new Set();
    const teachers = new Set();

    (list || []).forEach((item) => {
      if (item?.className) classes.add(item.className);
      if (item?.teacher) teachers.add(item.teacher);
    });

    classNames.value = Array.from(classes).sort();
    teacherNames.value = Array.from(teachers).sort();
  } catch (error) {
    ElMessage.error("获取班级/教师列表失败");
    console.error("获取班级/教师列表失败:", error);
  } finally {
    loadingOptions.value = false;
  }
};

// ========== 获取周课表 ==========
const fetchWeekSchedule = async () => {
  if (!selectedSemesterId.value) {
    ElMessage.warning("请选择学期");
    return;
  }
  if (!canQuery.value) {
    ElMessage.warning(viewMode.value === "class" ? "请选择班级" : "请选择教师");
    return;
  }

  loadingSchedule.value = true;
  try {
    if (viewMode.value === "class") {
      const response = await getWeekScheduleByClass(
          selectedSemesterId.value,
          selectedClassName.value
      );
      weekSchedule.value = response || [];
    } else {
      const response = await getWeekScheduleByTeacher(
          selectedSemesterId.value,
          selectedTeacher.value
      );
      weekSchedule.value = response || [];
    }
  } catch (error) {
    ElMessage.error("获取周课表失败");
    console.error("获取周课表失败:", error);
  } finally {
    loadingSchedule.value = false;
  }
};

// ========== 导出 ==========
const escapeCsvValue = (value) => {
  if (value === null || value === undefined) return "";
  const stringValue = String(value);
  if (stringValue.includes(",") || stringValue.includes("\"") || stringValue.includes("\n")) {
    return `"${stringValue.replace(/"/g, '""')}"`;
  }
  return stringValue;
};

const downloadCsv = (rows, filename) => {
  const content = rows.map((row) => row.map(escapeCsvValue).join(",")).join("\n");
  const blob = new Blob([`\ufeff${content}`], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

const handleExport = async () => {
  if (!canExport.value) {
    ElMessage.warning("请先选择学期与班级/教师并查询");
    return;
  }
  try {
    const schedules = await getCourseSchedules(selectedSemesterId.value);
    const filterValue =
        viewMode.value === "class" ? selectedClassName.value : selectedTeacher.value;
    const filtered = schedules.filter((schedule) =>
        viewMode.value === "class"
            ? schedule.className === filterValue
            : schedule.teacher === filterValue
    );
    if (filtered.length === 0) {
      ElMessage.warning("暂无可导出的课表数据");
      return;
    }
    const header = [
      "课程名",
      "学期",
      "星期",
      "节次",
      "上课时间",
      "教室",
      "教师",
      "班级",
    ];
    const rows = filtered.map((schedule) => [
      schedule.courseName || "",
      schedule.semesterId || "",
      schedule.weekDay || "",
      schedule.classSection || "",
      `${schedule.startTime || ""}-${schedule.endTime || ""}`,
      schedule.classroom || "",
      schedule.teacher || "",
      schedule.className || "",
    ]);
    const filename = `course_schedule_semester${selectedSemesterId.value}_${
        viewMode.value === "class" ? `class_${filterValue}` : `teacher_${filterValue}`
    }.csv`;
    downloadCsv([header, ...rows], filename);
    ElMessage.success("课表已导出");
  } catch (error) {
    ElMessage.error("导出课表失败");
    console.error("导出课表失败:", error);
  }
};

// 学期变化：刷新下拉选项，清空课表
watch(selectedSemesterId, async (newId) => {
  selectedClassName.value = "";
  selectedTeacher.value = "";
  weekSchedule.value = [];
  await fetchOptionsBySemester(newId);
});

// 视角变化：清空另一侧选择与课表
watch(viewMode, (mode) => {
  weekSchedule.value = [];
  if (mode === "class") selectedTeacher.value = "";
  else selectedClassName.value = "";
});

onMounted(async () => {
  await fetchSemesters();
  if (selectedSemesterId.value) {
    await fetchOptionsBySemester(selectedSemesterId.value);
  }
});
</script>

<style scoped>
/* ====== 页面容器 ====== */
.semester-schedule {
  padding: 4px 0;
}

.schedule-card {
  border-radius: 14px;
  overflow: hidden;
}

.mb-4 {
  margin-bottom: 16px;
}

/* ====== 头部 ====== */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
}

.title-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.title {
  font-weight: 700;
  font-size: 16px;
  line-height: 1.2;
}

.subtitle {
  font-size: 12px;
  color: #909399;
}

.header-actions {
  display: flex;
  align-items: center;
}

/* radio button 更像“切换器” */
.mode-group :deep(.el-radio-button__inner) {
  padding: 6px 12px;
  font-weight: 600;
}

/* ====== 周课表容器 ====== */
.week-schedule-container {
  margin-top: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

/* 顶部星期栏（含左上角占位） */
.week-header {
  display: grid;
  grid-template-columns: 90px repeat(7, minmax(140px, 1fr));
  gap: 10px;
  margin-bottom: 10px;
  align-items: stretch;
}

.week-head-empty {
  border-radius: 12px;
  background: transparent;
}

.week-day {
  background: linear-gradient(180deg, #f7f8fa, #f0f2f5);
  border: 1px solid rgba(0, 0, 0, 0.06);
  padding: 10px 8px;
  text-align: center;
  font-weight: 700;
  border-radius: 12px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.03);
}

.day-label {
  letter-spacing: 0.5px;
}

/* 内容区：左时间轴 + 右网格 */
.schedule-content {
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 10px;
}

/* 时间轴 */
.time-slot {
  display: grid;
  grid-template-rows: repeat(var(--rows, 5), 74px);
  gap: 10px;
}

.slot-time {
  height: 74px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  font-size: 12px;
  color: #606266;
  background: linear-gradient(180deg, #f7f8fa, #f0f2f5);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
}

/* 课程网格 */
.course-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(var(--cols, 7), minmax(140px, 1fr));
  grid-template-rows: repeat(var(--rows, 5), 74px);
  gap: 10px;
  min-width: calc(7 * 140px);
  --rows: 5;
  --cols: 7;
}

/* 背景格子 */
.grid-bg-cell {
  border-radius: 12px;
  background: #fafafa;
  border: 1px dashed rgba(0, 0, 0, 0.06);
  transition: background 0.15s ease;
}

.course-grid:hover .grid-bg-cell {
  background: #fbfbfb;
}

/* 课程块 */
.course-cell {
  position: relative;
  border-radius: 12px;
  padding: 10px 10px 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid rgba(0, 0, 0, 0.07);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
  transition: transform 0.16s ease, box-shadow 0.16s ease, filter 0.16s ease;
}

.course-cell::before {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 10%, rgba(255, 255, 255, 0.6), transparent 45%);
  pointer-events: none;
}

.course-cell:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.10);
  filter: saturate(1.05);
}

.course-info {
  position: relative;
  z-index: 1;
  width: 100%;
  text-align: left;
}

.course-name {
  font-weight: 800;
  font-size: 13px;
  margin-bottom: 6px;
  color: rgba(0, 0, 0, 0.78);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.62);
  overflow: hidden;
  white-space: nowrap;
}

.course-teacher,
.course-classroom {
  overflow: hidden;
  text-overflow: ellipsis;
}

.dot {
  opacity: 0.6;
}

.course-hover-hint {
  position: absolute;
  right: 10px;
  bottom: 8px;
  font-size: 11px;
  color: rgba(0, 0, 0, 0.55);
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.16s ease, transform 0.16s ease;
  z-index: 1;
}

.course-cell:hover .course-hover-hint {
  opacity: 1;
  transform: translateY(0);
}

/* 空提示 */
.empty-tip {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 13px;
  pointer-events: none;
  background: rgba(255, 255, 255, 0.55);
  border-radius: 12px;
}
</style>
