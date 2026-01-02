<template>
  <div class="practice-project-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <!-- 仅改标题：标题 + 副标题（其余不动） -->
          <div class="title-wrap">
            <div class="page-title">实践项目管理</div>
            <div class="page-subtitle">发布与维护实践项目，支持新增、编辑与截止时间管理</div>
          </div>

          <el-button type="primary" @click="openDialog">
            <el-icon><Plus /></el-icon> 添加项目
          </el-button>
        </div>
      </template>

      <el-table :data="projects" style="width: 100%">
        <el-table-column prop="projectId" label="项目ID" width="100" />
        <el-table-column prop="title" label="项目标题" min-width="200" />
        <el-table-column
            prop="description"
            label="项目描述"
            min-width="300"
            show-overflow-tooltip
        />
        <el-table-column prop="publisher" label="发布者" width="150" />
        <el-table-column
            prop="createTime"
            label="创建时间"
            width="200"
            :formatter="formatDateTime"
        />
        <el-table-column
            prop="deadline"
            label="截止时间"
            width="200"
            :formatter="formatDateTime"
        />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-button
                type="danger"
                size="small"
                @click="handleDelete(scope.row.projectId)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 项目表单对话框 -->
      <el-dialog v-model="dialogVisible" title="项目表单" width="600px">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
          <el-form-item label="项目标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入项目标题" />
          </el-form-item>
          <el-form-item label="项目描述" prop="description">
            <el-input
                v-model="form.description"
                type="textarea"
                rows="4"
                placeholder="请输入项目描述"
            />
          </el-form-item>
          <el-form-item label="发布者" prop="publisher">
            <el-input v-model="form.publisher" placeholder="请输入发布者" />
          </el-form-item>
          <el-form-item label="截止时间" prop="deadline">
            <el-date-picker
                v-model="form.deadline"
                type="datetime"
                placeholder="选择截止时间"
                style="width: 100%"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitForm">提交</el-button>
          </span>
        </template>
      </el-dialog>
    </el-card>

    <el-card class="mt-4">
      <template #header>
        <div class="card-header">
          <div class="title-wrap">
            <div class="page-title">实践课程表格</div>
            <div class="page-subtitle">根据培养方案与学期生成课程表格</div>
          </div>
          <el-button type="primary" @click="handleGenerateCourseTable">
            生成表格
          </el-button>
        </div>
      </template>

      <div class="table-filters">
        <el-select v-model="selectedProgramId" placeholder="选择培养方案" style="width: 240px">
          <el-option
              v-for="program in trainingPrograms"
              :key="program.programId"
              :label="`${program.majorName}(${program.effectiveYear})`"
              :value="program.programId"
          />
        </el-select>
        <el-select v-model="selectedSemester" placeholder="选择学期" style="width: 160px">
          <el-option
              v-for="semester in semesterOptions"
              :key="semester.value"
              :label="semester.label"
              :value="semester.value"
          />
        </el-select>
      </div>

      <el-table
          v-if="practiceCourseTable.length"
          :data="practiceCourseTable"
          border
          stripe
          style="width: 100%"
      >
        <el-table-column prop="courseName" label="课程名称" min-width="160" />
        <el-table-column prop="courseCode" label="课程代码" width="130" />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column prop="totalHours" label="总学时" width="100" />
        <el-table-column prop="theoreticalHours" label="理论学时" width="100" />
        <el-table-column prop="practicalHours" label="实践学时" width="100" />
        <el-table-column prop="courseType" label="课程类型" width="100" />
        <el-table-column prop="courseNature" label="课程性质" width="100" />
        <el-table-column prop="examMark" label="考核方式" width="100" />
        <el-table-column prop="courseCategory" label="课程类别" width="120" />
      </el-table>
      <el-empty v-else description="请选择培养方案与学期生成表格" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getPracticeProjects,
  addPracticeProject,
  updatePracticeProject,
  deletePracticeProject
} from '@/api/practiceProject'
import { getAllTrainingPrograms } from '@/api/trainingProgram'
import { getCoursesByProgramAndSemester } from '@/api/course'

// 项目列表
const projects = ref([])
// 对话框可见性
const dialogVisible = ref(false)
// 表单引用
const formRef = ref(null)
// 表单数据
const form = ref({
  projectId: null,
  title: '',
  description: '',
  publisher: '',
  deadline: null
})

const trainingPrograms = ref([])
const selectedProgramId = ref(null)
const selectedSemester = ref(null)
const practiceCourseTable = ref([])
const semesterOptions = [
  { value: 1, label: '第1学期' },
  { value: 2, label: '第2学期' },
  { value: 3, label: '第3学期' },
  { value: 4, label: '第4学期' },
  { value: 5, label: '第5学期' },
  { value: 6, label: '第6学期' },
  { value: 7, label: '第7学期' },
  { value: 8, label: '第8学期' }
]
// 表单验证规则
const rules = ref({
  title: [{ required: true, message: '请输入项目标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入项目描述', trigger: 'blur' }],
  publisher: [{ required: true, message: '请输入发布者', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }]
})

// 格式化日期时间
const formatDateTime = (row, column, cellValue) => {
  if (!cellValue) return ''
  const date = new Date(cellValue)
  return date.toLocaleString()
}

// 打开对话框
const openDialog = (project = null) => {
  if (project) {
    // 编辑模式
    form.value = { ...project }
  } else {
    // 新增模式
    form.value = {
      projectId: null,
      title: '',
      description: '',
      publisher: '',
      deadline: null
    }
  }
  dialogVisible.value = true
}

// 编辑项目
const handleEdit = (project) => {
  openDialog(project)
}

// 删除项目
const handleDelete = (projectId) => {
  ElMessage.confirm('确定要删除这个项目吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
      .then(() => {
        deletePracticeProject(projectId)
            .then(() => {
              ElMessage.success('删除成功')
              loadProjects()
            })
            .catch(() => {
              ElMessage.error('删除失败')
            })
      })
      .catch(() => {
        // 取消删除
      })
}

// 提交表单
const submitForm = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      if (form.value.projectId) {
        // 更新项目
        updatePracticeProject(form.value.projectId, form.value)
            .then(() => {
              ElMessage.success('更新成功')
              dialogVisible.value = false
              loadProjects()
            })
            .catch(() => {
              ElMessage.error('更新失败')
            })
      } else {
        // 新增项目
        addPracticeProject(form.value)
            .then(() => {
              ElMessage.success('添加成功')
              dialogVisible.value = false
              loadProjects()
            })
            .catch(() => {
              ElMessage.error('添加失败')
            })
      }
    }
  })
}

const loadTrainingPrograms = () => {
  getAllTrainingPrograms()
      .then((res) => {
        trainingPrograms.value = res
      })
      .catch(() => {
        ElMessage.error('获取培养方案失败')
      })
}

const handleGenerateCourseTable = () => {
  if (!selectedProgramId.value || !selectedSemester.value) {
    ElMessage.warning('请先选择培养方案与学期')
    return
  }
  getCoursesByProgramAndSemester(
      selectedProgramId.value,
      selectedSemester.value
  )
      .then((res) => {
        practiceCourseTable.value = res
        ElMessage.success('课程表格已生成')
      })
      .catch(() => {
        ElMessage.error('生成课程表格失败')
      })
}

// 加载项目列表
const loadProjects = () => {
  getPracticeProjects().then((res) => {
    projects.value = res
  })
}

// 组件挂载时加载数据
onMounted(() => {
  loadProjects()
  loadTrainingPrograms()
})
</script>

<style scoped>
.mt-4 {
  margin-top: 16px;
}

.table-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 新增：仅标题风格（不影响其它区域） */
.title-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.page-title {
  font-size: 16px;
  font-weight: 900;
  color: rgba(0, 0, 0, 0.86);
  line-height: 1.2;
}
.page-subtitle {
  font-size: 12px;
  color: #909399;
  line-height: 1.2;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
