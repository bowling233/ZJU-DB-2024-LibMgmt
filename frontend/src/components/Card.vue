<template>
  <el-scrollbar height="100%" style="width: 100%">
    <!-- 标题和搜索框 -->
    <div
      style="
        margin-top: 20px;
        margin-left: 40px;
        font-size: 2em;
        font-weight: bold;
      "
    >
      借书证管理
      <el-input
        v-model="toSearch"
        style="
          width: 15vw;
          min-width: 150px;
          margin-left: 30px;
          margin-right: 30px;
          float: right;
        "
	:prefix-icon="Search"
	placeholder="姓名检索"
        clearable
      />
    </div>

    <!-- 借书证卡片显示区 -->
    <div style="display: flex; flex-wrap: wrap; justify-content: start">
      <!-- 借书证卡片 -->
      <div
        class="cardBox"
        v-for="card in cards"
        v-show="card.name.includes(toSearch)"
        :key="card.id"
      >
        <div>
          <!-- 卡片标题 -->
          <div style="font-size: 25px; font-weight: bold">
            No. {{ card.id }}
          </div>

          <el-divider />

          <!-- 卡片内容 -->
          <div style="margin-left: 10px; text-align: start; font-size: 16px;
		      text-overflow: ellipsis; white-space: nowrap;">
            <p style="padding: 2.5px">
              <span style="font-weight: bold">姓名：</span>{{ card.name }}
            </p>
            <p style="padding: 2.5px">
              <span style="font-weight: bold">部门：</span>{{ card.department }}
            </p>
            <p style="padding: 2.5px">
              <span style="font-weight: bold">类型：</span
              >{{ card.type === "S" ? "学生" : "老师" }}
            </p>
          </div>

          <el-divider />

          <!-- 卡片操作 -->
          <div style="margin-top: 10px">
            <el-button
              type="primary"
              @click="
                (this.toModifyInfo.id = card.id),
                  (this.toModifyInfo.name = card.name),
                  (this.toModifyInfo.department = card.department),
                  (this.toModifyInfo.type = card.type),
                  (this.modifyCardVisible = true)
              "
              circle
            ><el-icon> <Edit /> </el-icon> </el-button>
            <el-button
              type="danger"
              circle
              @click="
                (this.toRemove = card.id), (this.removeCardVisible = true)
              "
              style="margin-left: 30px"
            ><el-icon> <Delete /> </el-icon> </el-button>
          </div>
        </div>
      </div>

      <!-- 新建借书证卡片 -->
      <el-button
        class="newCardBox"
        @click="
          (newCardInfo.name = ''),
            (newCardInfo.department = ''),
            (newCardInfo.type = 'S'),
            (newCardVisible = true)
        "
      >
        <el-icon style="height: 50px; width: 50px">
          <Plus style="height: 100%; width: 100%" />
        </el-icon>
      </el-button>
    </div>

    <!-- 新建借书证对话框 -->
    <el-dialog
      v-model="newCardVisible"
      title="新建借书证"
      width="30%"
      align-center
    >
      <div
        style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        姓名：
        <el-input v-model="newCardInfo.name" style="width: 12.5vw" clearable />
      </div>
      <div
        style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        部门：
        <el-input
          v-model="newCardInfo.department"
          style="width: 12.5vw"
          clearable
        />
      </div>
      <div
        style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        类型：
        <el-select
          v-model="newCardInfo.type"
          size="middle"
          style="width: 12.5vw"
        >
          <el-option
            v-for="type in types"
            :key="type.value"
            :label="type.label"
            :value="type.value"
          />
        </el-select>
      </div>

      <template #footer>
        <span>
          <el-button @click="newCardVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="ConfirmNewCard"
            :disabled="
              newCardInfo.name.length === 0 ||
              newCardInfo.department.length === 0
            "
            >确定</el-button
          >
        </span>
      </template>
    </el-dialog>

    <!-- 修改信息对话框 -->
    <el-dialog
      v-model="modifyCardVisible"
      :title="'修改信息(借书证ID: ' + this.toModifyInfo.id + ')'"
      width="30%"
      align-center
    >
      <div
        style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        姓名：
        <el-input v-model="toModifyInfo.name" style="width: 12.5vw" clearable />
      </div>
      <div
        style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        部门：
        <el-input
          v-model="toModifyInfo.department"
          style="width: 12.5vw"
          clearable
        />
      </div>
      <div
        style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        类型：
        <el-select
          v-model="toModifyInfo.type"
          size="middle"
          style="width: 12.5vw"
        >
          <el-option
            v-for="type in types"
            :key="type.value"
            :label="type.label"
            :value="type.value"
          />
        </el-select>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="modifyCardVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="ConfirmModifyCard"
            :disabled="
              toModifyInfo.name.length === 0 ||
              toModifyInfo.department.length === 0
            "
            >确定</el-button
          >
        </span>
      </template>
    </el-dialog>

    <!-- 删除借书证对话框 -->
    <el-dialog v-model="removeCardVisible" title="删除借书证" width="30%">
      <span
        >确定删除<span style="font-weight: bold">{{ toRemove }}号借书证</span
        >吗？</span
      >

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="removeCardVisible = false">取消</el-button>
          <el-button type="danger" @click="ConfirmRemoveCard"> 删除 </el-button>
        </span>
      </template>
    </el-dialog>
  </el-scrollbar>
</template>

<script>
import { ref } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from "element-plus";
import axios from "axios";


export default {
  data() {
    return {
      cards: [],
      toSearch: "", // 搜索内容
      Search,
      types: [
        // 借书证类型
        {
          value: "T",
          label: "教师",
        },
        {
          value: "S",
          label: "学生",
        },
      ],
      newCardVisible: false, // 新建借书证对话框可见性
      removeCardVisible: false, // 删除借书证对话框可见性
      toRemove: 0, // 待删除借书证号
      newCardInfo: {
        // 待新建借书证信息
        name: "",
        department: "",
        type: "S",
      },
      modifyCardVisible: false, // 修改信息对话框可见性
      toModifyInfo: {
        // 待修改借书证信息
        id: 0,
        name: "",
        department: "",
        type: "S",
      },
    };
  },
  methods: {
    async ConfirmNewCard() {
      axios
        .post("/cards", {
          name: this.newCardInfo.name,
          department: this.newCardInfo.department,
          type: this.newCardInfo.type,
        })
        .then(() => {
          this.Success();
          this.newCardVisible = false;
          this.QueryCards();
        })
        .catch((error) => {
          this.ErrorHandling(error);
        });
    },
    ConfirmModifyCard() {
      axios
        .put(`/cards/${this.toModifyInfo.id}`, {
          cardId: this.toModifyInfo.id,
          name: this.toModifyInfo.name,
          department: this.toModifyInfo.department,
          type: this.toModifyInfo.type,
        })
        .then(() => {
          this.Success();
          this.modifyCardVisible = false;
          this.QueryCards();
        })
        .catch((error) => {
          this.ErrorHandling(error);
        });
    },
    ConfirmRemoveCard() {
      axios
        .delete(`/cards/${this.toRemove}`, {
          cardId: this.toRemove,
        })
        .then(() => {
          this.Success();
          this.removeCardVisible = false;
          this.QueryCards();
        })
        .catch((error) => {
          this.ErrorHandling(error);
        });
    },
    QueryCards() {
      this.cards = [];
      axios.get("/cards").then((response) => {
        this.cards = response.data;
      });
    },
    Success() {
      ElMessage.success("操作成功");
    },
    ErrorHandling(error) {
      if (error.response) {
        ElMessage.error("操作失败：" + error.response.data);
        console.log(error.response.data);
        console.log(error.response.status);
        console.log(error.response.headers);
      } else if (error.request) {
        ElMessage.error("服务器无返回：" + error.request);
        console.log(error.request);
      } else {
        ElMessage.error("网页内部错误：" + error.message);
        console.log("Error", error.message);
      }
    },
  },
  mounted() {
    this.QueryCards();
  },
};
</script>

<style scoped>
.cardBox {
  height: 300px;
  width: 200px;
  box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
  text-align: center;
  margin-top: 40px;
  margin-left: 27.5px;
  margin-right: 10px;
  padding: 7.5px;
  padding-right: 10px;
  padding-top: 15px;
}

.newCardBox {
  height: 300px;
  width: 200px;
  margin-top: 40px;
  margin-left: 27.5px;
  margin-right: 10px;
  padding: 7.5px;
  padding-right: 10px;
  padding-top: 15px;
  box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
  text-align: center;
}
</style>
