<template>
  <div class="statistics-page">
    <el-card class="page-card">
      <template #header>
        <div class="page-header">
          <div class="title-wrap">
            <span class="page-title">系统统计与报表</span>
            <span class="page-subtitle">课程、知识点、题库、试卷汇总概览</span>
          </div>

          <div class="header-actions">
            <el-button
                type="primary"
                :loading="loadingCourseStatistics || loadingExamPaperStatistics"
                @click="fetchAllStatistics"
            >
              刷新数据
            </el-button>
          </div>
        </div>
      </template>

      <div class="dashboard">
        <!-- 统计卡片 -->
        <div class="stat-grid">
          <div class="stat-tile stat-tile--blue">
            <div class="stat-icon">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">课程总数</div>
              <div class="stat-value">{{ courseStatistics.totalCourses }}</div>
            </div>
          </div>

          <div class="stat-tile stat-tile--amber">
            <div class="stat-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">知识点总数</div>
              <div class="stat-value">
                {{ knowledgePointStatistics.totalKnowledgePoints }}
              </div>
            </div>
          </div>

          <div class="stat-tile stat-tile--purple">
            <div class="stat-icon">
              <el-icon><EditPen /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">题目总数</div>
              <div class="stat-value">{{ questionBankStatistics.totalQuestions }}</div>
            </div>
          </div>

          <div class="stat-tile stat-tile--red">
            <div class="stat-icon">
              <el-icon><DocumentRemove /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">试卷总数</div>
              <div class="stat-value">{{ examPaperStatistics.totalPapers }}</div>
            </div>
          </div>

          <div class="stat-tile stat-tile--green">
            <div class="stat-icon">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">课程平均学分</div>
              <div class="stat-value">
                {{ Number(courseStatistics.averageCredit || 0).toFixed(1) }}
              </div>
            </div>
          </div>

          <div class="stat-tile stat-tile--indigo">
            <div class="stat-icon">
              <el-icon><DocumentRemove /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">试卷平均总分</div>
              <div class="stat-value">
                {{ Number(examPaperStatistics.averageTotalScore || 0).toFixed(1) }}
              </div>
            </div>
          </div>
        </div>

        <!-- 报表卡片 -->
        <div class="report-grid">
          <!-- 题型分布 -->
          <el-card class="report-card">
            <template #header>
              <div class="report-header">
                <span class="report-title">题型分布</span>
                <span class="report-badge" v-if="questionTypeDist.length">
                  {{ questionTypeDist.length }} 项
                </span>
              </div>
            </template>

            <el-table
                v-if="questionTypeDist.length"
                class="table"
                :data="questionTypeDist"
                border
                stripe
            >
              <el-table-column
                  prop="type"
                  label="题型"
                  width="140"
                  :formatter="formatQuestionType"
              />
              <el-table-column prop="count" label="数量" width="120" />
            </el-table>
            <el-empty v-else description="暂无数据" />
          </el-card>

          <!-- 课程分布（培养方案） -->
          <el-card class="report-card">
            <template #header>
              <div class="report-header">
                <span class="report-title">课程分布（培养方案）</span>
                <span class="report-badge" v-if="courseByProgram.length">
                  {{ courseByProgram.length }} 项
                </span>
              </div>
            </template>

            <template v-if="courseByProgram.length">
              <el-table class="table" :data="visibleCourseByProgram" border stripe>
                <el-table-column prop="programName" label="培养方案" />
                <el-table-column prop="courseCount" label="课程数量" width="120" />
              </el-table>

              <div v-if="courseByProgram.length > tablePreviewLimit" class="table-actions">
                <el-button type="primary" link @click="toggleCourseByProgram">
                  {{ showAllCourseByProgram ? "收起" : "查看更多" }}
                </el-button>
              </div>
              <div v-else class="table-actions table-actions--spacer"></div>
            </template>

            <el-empty v-else-if="!loadingCourseStatistics" description="暂无数据" />
            <div v-else class="skeleton" />
          </el-card>

          <!-- 课程分布（课程性质） -->
          <el-card class="report-card">
            <template #header>
              <div class="report-header">
                <span class="report-title">课程分布（课程性质）</span>
                <span class="report-badge" v-if="courseByNature.length">
                  {{ courseByNature.length }} 项
                </span>
              </div>
            </template>

            <template v-if="courseByNature.length">
              <el-table class="table" :data="visibleCourseByNature" border stripe>
                <el-table-column prop="courseNature" label="课程性质" />
                <el-table-column prop="courseCount" label="数量" width="120" />
              </el-table>

              <div v-if="courseByNature.length > tablePreviewLimit" class="table-actions">
                <el-button type="primary" link @click="toggleCourseByNature">
                  {{ showAllCourseByNature ? "收起" : "查看更多" }}
                </el-button>
              </div>
              <div v-else class="table-actions table-actions--spacer"></div>
            </template>

            <el-empty v-else-if="!loadingCourseStatistics" description="暂无数据" />
            <div v-else class="skeleton" />
          </el-card>

          <!-- 知识点覆盖（课程维度） -->
          <el-card class="report-card">
            <template #header>
              <div class="report-header">
                <span class="report-title">知识点覆盖（课程维度）</span>
                <span class="report-badge" v-if="knowledgePointByCourse.length">
                  {{ knowledgePointByCourse.length }} 项
                </span>
              </div>
            </template>

            <template v-if="knowledgePointByCourse.length">
              <el-table
                  class="table"
                  :data="visibleKnowledgePointByCourse"
                  border
                  stripe
              >
                <el-table-column prop="courseName" label="课程" />
                <el-table-column
                    prop="knowledgePointCount"
                    label="知识点数量"
                    width="120"
                />
                <el-table-column prop="questionCount" label="题目数量" width="120" />
              </el-table>

              <div
                  v-if="knowledgePointByCourse.length > tablePreviewLimit"
                  class="table-actions"
              >
                <el-button type="primary" link @click="toggleKnowledgePointByCourse">
                  {{ showAllKnowledgePointByCourse ? "收起" : "查看更多" }}
                </el-button>
              </div>
              <div v-else class="table-actions table-actions--spacer"></div>
            </template>

            <el-empty v-else description="暂无数据" />
          </el-card>

          <!-- 知识点覆盖（知识点维度） -->
          <el-card class="report-card">
            <template #header>
              <div class="report-header">
                <span class="report-title">知识点覆盖（知识点维度）</span>
                <span class="report-badge" v-if="knowledgePointCoverage.length">
                  {{ knowledgePointCoverage.length }} 项
                </span>
              </div>
            </template>

            <template v-if="knowledgePointCoverage.length">
              <el-table class="table" :data="visibleKnowledgePointCoverage" border stripe>
                <el-table-column prop="kpName" label="知识点" />
                <el-table-column prop="courseName" label="课程" />
                <el-table-column prop="questionCount" label="题目数量" width="120" />
              </el-table>

              <div
                  v-if="knowledgePointCoverage.length > tablePreviewLimit"
                  class="table-actions"
              >
                <el-button type="primary" link @click="toggleKnowledgePointCoverage">
                  {{ showAllKnowledgePointCoverage ? "收起" : "查看更多" }}
                </el-button>
              </div>
              <div v-else class="table-actions table-actions--spacer"></div>
            </template>

            <el-empty v-else description="暂无数据" />
          </el-card>

          <!-- 题目难度分布 -->
          <el-card class="report-card">
            <template #header>
              <div class="report-header">
                <span class="report-title">题目难度分布</span>
                <span class="report-badge" v-if="questionDifficultyDist.length">
                  {{ questionDifficultyDist.length }} 项
                </span>
              </div>
            </template>

            <el-table
                v-if="questionDifficultyDist.length"
                class="table"
                :data="questionDifficultyDist"
                border
                stripe
            >
              <el-table-column prop="difficulty" label="难度" />
              <el-table-column prop="count" label="数量" width="120" />
            </el-table>
            <el-empty v-else description="暂无数据" />
          </el-card>

          <!-- 试卷类型分布 -->
          <el-card class="report-card">
            <template #header>
              <div class="report-header">
                <span class="report-title">试卷类型分布</span>
                <span class="report-badge" v-if="paperTypeDist.length">
                  {{ paperTypeDist.length }} 项
                </span>
              </div>
            </template>

            <el-table v-if="paperTypeDist.length" class="table" :data="paperTypeDist" border stripe>
              <el-table-column prop="type" label="类型" />
              <el-table-column prop="count" label="数量" width="120" />
            </el-table>
            <el-empty v-else description="暂无数据" />
          </el-card>

          <!-- 试卷课程分布 -->
          <el-card class="report-card">
            <template #header>
              <div class="report-header">
                <span class="report-title">试卷课程分布</span>
                <span class="report-badge" v-if="papersByCourse.length">
                  {{ papersByCourse.length }} 项
                </span>
              </div>
            </template>

            <template v-if="papersByCourse.length">
              <el-table class="table" :data="visiblePapersByCourse" border stripe>
                <el-table-column prop="courseName" label="课程" />
                <el-table-column prop="paperCount" label="试卷数量" width="120" />
              </el-table>

              <div v-if="papersByCourse.length > tablePreviewLimit" class="table-actions">
                <el-button type="primary" link @click="togglePapersByCourse">
                  {{ showAllPapersByCourse ? "收起" : "查看更多" }}
                </el-button>
              </div>
              <div v-else class="table-actions table-actions--spacer"></div>
            </template>

            <el-empty v-else-if="!loadingExamPaperStatistics" description="暂无数据" />
            <div v-else class="skeleton" />
          </el-card>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { Cpu, Document, EditPen, DocumentRemove } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import request from "../api/request";

// 统计数据
const courseStatistics = ref({
  totalCourses: 0,
  averageCredit: 0,
  courseByProgram: [],
  courseByNature: [],
});

const knowledgePointStatistics = ref({
  totalKnowledgePoints: 0,
  coverageByKnowledgePoint: [],
  coverageByCourse: [],
});

const questionBankStatistics = ref({
  totalQuestions: 0,
  questionTypeDistribution: [],
  questionDifficultyDistribution: [],
});

const examPaperStatistics = ref({
  totalPapers: 0,
  averageTotalScore: 0,
  paperTypeDistribution: [],
  papersByCourse: [],
});

const questionTypeDist = ref([]);
const questionDifficultyDist = ref([]);
const courseByProgram = ref([]);
const courseByNature = ref([]);
const knowledgePointCoverage = ref([]);
const knowledgePointByCourse = ref([]);
const paperTypeDist = ref([]);
const papersByCourse = ref([]);

const tablePreviewLimit = 5;

const loadingCourseStatistics = ref(true);
const loadingExamPaperStatistics = ref(true);

const showAllCourseByProgram = ref(false);
const showAllCourseByNature = ref(false);
const showAllKnowledgePointCoverage = ref(false);
const showAllKnowledgePointByCourse = ref(false);
const showAllPapersByCourse = ref(false);

const visibleCourseByProgram = computed(() =>
    showAllCourseByProgram.value
        ? courseByProgram.value
        : courseByProgram.value.slice(0, tablePreviewLimit)
);
const visibleCourseByNature = computed(() =>
    showAllCourseByNature.value
        ? courseByNature.value
        : courseByNature.value.slice(0, tablePreviewLimit)
);
const visibleKnowledgePointCoverage = computed(() =>
    showAllKnowledgePointCoverage.value
        ? knowledgePointCoverage.value
        : knowledgePointCoverage.value.slice(0, tablePreviewLimit)
);
const visibleKnowledgePointByCourse = computed(() =>
    showAllKnowledgePointByCourse.value
        ? knowledgePointByCourse.value
        : knowledgePointByCourse.value.slice(0, tablePreviewLimit)
);
const visiblePapersByCourse = computed(() =>
    showAllPapersByCourse.value
        ? papersByCourse.value
        : papersByCourse.value.slice(0, tablePreviewLimit)
);

const toggleCourseByProgram = () => (showAllCourseByProgram.value = !showAllCourseByProgram.value);
const toggleCourseByNature = () => (showAllCourseByNature.value = !showAllCourseByNature.value);
const toggleKnowledgePointCoverage = () =>
    (showAllKnowledgePointCoverage.value = !showAllKnowledgePointCoverage.value);
const toggleKnowledgePointByCourse = () =>
    (showAllKnowledgePointByCourse.value = !showAllKnowledgePointByCourse.value);
const togglePapersByCourse = () => (showAllPapersByCourse.value = !showAllPapersByCourse.value);

const unwrapResponse = (response) => response?.data ?? response ?? {};

// 获取课程统计数据
const fetchCourseStatistics = async () => {
  loadingCourseStatistics.value = true;
  try {
    const response = await request({ url: "/statistics/course", method: "GET" });
    const payload = unwrapResponse(response);
    courseStatistics.value = payload;
    courseByProgram.value = payload.courseByProgram || [];
    courseByNature.value = payload.courseByNature || [];
  } catch (error) {
    ElMessage.error("获取课程统计数据失败");
    console.error("获取课程统计数据失败:", error);
  } finally {
    loadingCourseStatistics.value = false;
  }
};

// 获取知识点统计数据
const fetchKnowledgePointStatistics = async () => {
  try {
    const response = await request({ url: "/statistics/knowledge-point", method: "GET" });
    const payload = unwrapResponse(response);
    knowledgePointStatistics.value = payload;
    knowledgePointCoverage.value = payload.coverageByKnowledgePoint || [];
    knowledgePointByCourse.value = payload.coverageByCourse || [];
  } catch (error) {
    ElMessage.error("获取知识点统计数据失败");
    console.error("获取知识点统计数据失败:", error);
  }
};

// 获取题库统计数据
const fetchQuestionBankStatistics = async () => {
  try {
    const response = await request({ url: "/statistics/question-bank", method: "GET" });
    const payload = unwrapResponse(response);
    questionBankStatistics.value = payload;
    questionTypeDist.value = payload.questionTypeDistribution || [];
    questionDifficultyDist.value = payload.questionDifficultyDistribution || [];
  } catch (error) {
    ElMessage.error("获取题库统计数据失败");
    console.error("获取题库统计数据失败:", error);
  }
};

// 获取试卷统计数据
const fetchExamPaperStatistics = async () => {
  loadingExamPaperStatistics.value = true;
  try {
    const response = await request({ url: "/statistics/exam-paper", method: "GET" });
    const payload = unwrapResponse(response);
    examPaperStatistics.value = payload;
    paperTypeDist.value = payload.paperTypeDistribution || [];
    papersByCourse.value = payload.papersByCourse || [];
  } catch (error) {
    ElMessage.error("获取试卷统计数据失败");
    console.error("获取试卷统计数据失败:", error);
  } finally {
    loadingExamPaperStatistics.value = false;
  }
};

// 获取所有统计数据
const fetchAllStatistics = async () => {
  await Promise.all([
    fetchCourseStatistics(),
    fetchKnowledgePointStatistics(),
    fetchQuestionBankStatistics(),
    fetchExamPaperStatistics(),
  ]);
};

// 将题型编号转换为名称
const formatQuestionType = (row, column, cellValue) => {
  const typeMap = { 1: "单选题", 2: "多选题", 3: "判断题", 4: "填空题", 5: "简答题" };
  return typeMap[cellValue] || "其他";
};

onMounted(async () => {
  await fetchAllStatistics();
});
</script>

<style scoped>
/* ====== 页面与顶层卡片（与整体 Layout 玻璃风一致） ====== */
.statistics-page {
  width: 100%;
}

.page-card {
  border-radius: 16px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.08);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.title-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.page-title {
  font-size: 16px;
  font-weight: 800;
  color: rgba(0, 0, 0, 0.86);
}

.page-subtitle {
  font-size: 12px;
  color: #909399;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ====== dashboard ====== */
.dashboard {
  width: 100%;
}

/* 统计 tile：更像仪表盘 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(160px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.stat-tile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 14px;
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.06);
  transition: transform 0.16s ease, box-shadow 0.16s ease;
  position: relative;
  overflow: hidden;
}

.stat-tile::before {
  content: "";
  position: absolute;
  inset: -40px -60px auto auto;
  width: 160px;
  height: 160px;
  border-radius: 999px;
  opacity: 0.18;
  filter: blur(0px);
  background: currentColor;
  transform: translate(30px, -30px);
  pointer-events: none;
}

.stat-tile:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: #fff;
  flex: 0 0 auto;
  box-shadow: 0 12px 20px rgba(0, 0, 0, 0.10);
}

.stat-icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.62);
  font-weight: 700;
}

.stat-value {
  font-size: 20px;
  font-weight: 900;
  color: rgba(0, 0, 0, 0.82);
  line-height: 1.1;
}

/* 不同色系 */
.stat-tile--blue {
  color: #409eff;
}
.stat-tile--blue .stat-icon {
  background: linear-gradient(135deg, #409eff, #6aa8ff);
}

.stat-tile--amber {
  color: #e6a23c;
}
.stat-tile--amber .stat-icon {
  background: linear-gradient(135deg, #e6a23c, #f3c77a);
}

.stat-tile--purple {
  color: #8e5ef7;
}
.stat-tile--purple .stat-icon {
  background: linear-gradient(135deg, #8e5ef7, #b49bff);
}

.stat-tile--red {
  color: #f56c6c;
}
.stat-tile--red .stat-icon {
  background: linear-gradient(135deg, #f56c6c, #ff9a9a);
}

.stat-tile--green {
  color: #67c23a;
}
.stat-tile--green .stat-icon {
  background: linear-gradient(135deg, #67c23a, #9be27b);
}

.stat-tile--indigo {
  color: #5b7cff;
}
.stat-tile--indigo .stat-icon {
  background: linear-gradient(135deg, #5b7cff, #91a6ff);
}

/* ====== 报表区 ====== */
.report-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
  gap: 14px;
}

.report-card {
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(10px);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.06);
}

.report-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.report-title {
  font-weight: 900;
  color: rgba(0, 0, 0, 0.82);
}

.report-badge {
  font-size: 12px;
  color: #409eff;
  background: rgba(64, 158, 255, 0.12);
  border: 1px solid rgba(64, 158, 255, 0.18);
  padding: 2px 10px;
  border-radius: 999px;
}

/* 表格统一 */
.table :deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}

.table-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed rgba(0, 0, 0, 0.08);
  min-height: 32px;
}

.table-actions--spacer {
  visibility: hidden;
}

/* 简单 skeleton 占位 */
.skeleton {
  height: 120px;
  border-radius: 12px;
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.03), rgba(0, 0, 0, 0.06), rgba(0, 0, 0, 0.03));
  background-size: 300% 100%;
  animation: shimmer 1.2s ease-in-out infinite;
}

@keyframes shimmer {
  0% {
    background-position: 0% 0;
  }
  100% {
    background-position: 100% 0;
  }
}

/* ====== 响应式 ====== */
@media (max-width: 1400px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(160px, 1fr));
  }
}

@media (max-width: 860px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }
  .report-grid {
    grid-template-columns: 1fr;
  }
}
</style>
