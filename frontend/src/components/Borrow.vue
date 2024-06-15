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
      借书记录查询
    </div>
    <!-- 查询框 -->
    <div
      style="
        width: 90%;
        margin: 0 auto;
        padding-top: 5vh;
        display: flex;
        gap: 10px 20px;
        flex-wrap: wrap;
      "
    >
      <el-input
        style="flex: 1; min-width: 150px"
        v-model="this.toQuery"
        :prefix-icon="Search"
        placeholder="输入借书证ID"
        @change="QueryBorrows"
        clearable
      ></el-input>
      <el-input
        style="flex: 1; min-width: 150px"
        v-model="toSearch"
        :prefix-icon="Search"
        placeholder="输入书名或日期筛选"
        @change="filterFields"
        clearable
      />
      <el-button
        style="flex: 1; min-width: 150px"
        type="primary"
        @click="QueryBorrows"
        >查询</el-button
      >
    </div>

    <!-- 结果表格 -->
    <el-table
      v-if="isShow"
      :data="fitlerTableData"
      height="600"
      :default-sort="{ prop: 'borrowTime', order: 'ascending' }"
      :table-layout="'auto'"
      style="
        width: 100%;
        margin-left: 50px;
        margin-top: 30px;
        margin-right: 50px;
        max-width: 80vw;
      "
    >
      <el-table-column prop="cardId" label="借书证ID" />
      <el-table-column prop="bookId" label="图书ID" sortable />
      <el-table-column prop="borrowTime" label="借出时间" sortable />
      <el-table-column prop="returnTime" label="归还时间" sortable />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button
            style="margin-left: 2%; width: 80px"
            type="primary"
            @click="
              this.returnBookInfo.bookId = scope.row.bookId;
              this.returnBookInfo.cardId = scope.row.cardId;
              this.returnBookInfo.borrowTimeH = scope.row.borrowTime;
              this.returnBookVisible = true;
              convertToTimestamp();
            "
            v-if="scope.row.returnTime === 'null'"
            >归还</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="returnBookVisible"
      :title="'归还 bookId 为: ' + this.returnBookInfo.bookId + '的书本'"
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
        归还时间
        <el-input
          v-model="returnBookInfo.returnTimeH"
          style="width: 12.5vw; margin-left: 40px"
          clearable
          @input="convertToTimestamp"
        ></el-input>
      </div>
      <template #footer>
        <span>
          <el-button @click="returnBookVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="ConfirmReturnBook"
            :disabled="(!isNaN(returnBookInfo.returnTime)) && (returnBookInfo.returnTime <= returnBookInfo.borrowTime)"
            >确定</el-button
          >
        </span>
      </template>
    </el-dialog>
  </el-scrollbar>
</template>

<script>
import axios from "axios";
import { ElMessage } from "element-plus";
import { Search } from "@element-plus/icons-vue";
import { filterFields } from "element-plus/es/components/form/src/utils";

export default {
  data() {
    return {
      isShow: false, // 结果表格展示状态
      tableData: [
        {
          // 列表项
          cardId: 1,
          bookId: 1,
          borrowTime: "2012-03-08T12:17:24.000Z",
          returnTime: "2012-03-08T12:17:24.000Z",
        },
      ],
      toQuery: "", // 待查询内容(对某一借书证号进行查询)
      toSearch: "", // 待搜索内容(对查询到的结果进行搜索)

      returnBookVisible: false,
      returnBookInfo: {
        bookId: 0,
        cardId: 0,
        borrowTime: 0,
        borrowTimeH: new Date().toISOString(),
        returnTime: 0,
        returnTimeH: new Date().toISOString(),
      },
      Search,
    };
  },
  computed: {
    fitlerTableData() {
      // 搜索规则
      return this.tableData.filter(
        (tuple) =>
          this.toSearch == "" || // 搜索框为空，即不搜索
          tuple.bookId == this.toSearch || // 图书号与搜索要求一致
          tuple.borrowTime.toString().includes(this.toSearch) || // 借出时间包含搜索要求
          tuple.returnTime.toString().includes(this.toSearch) // 归还时间包含搜索要求
      );
    },
  },
  methods: {
    async QueryBorrows() {
      this.tableData = [];
      let response = await axios
        .get(`/cards/${this.toQuery}/borrows`, {})
        .catch((error) => {
          this.ErrorHandling(error);
        });
      let borrows = response.data;
      borrows.forEach((borrow) => {
	borrow.borrowTime = new Date(borrow.borrowTime).toISOString();
	if (borrow.returnTime != 0)
		borrow.returnTime = new Date(borrow.returnTime).toISOString();
	else
		borrow.returnTime = "null";
        this.tableData.push(borrow);
      });
      this.isShow = true;
    },
    ConfirmReturnBook() {
      axios
        .delete(`/cards/${this.returnBookInfo.cardId}/borrows`,
	{
          data: {
            bookId: this.returnBookInfo.bookId,
            cardId: this.returnBookInfo.cardId,
            borrowTime: this.returnBookInfo.borrowTime,
            returnTime: this.returnBookInfo.returnTime,
          },
        })
        .then(() => {
          this.Success();
          this.returnBookVisible = false;
          this.QueryBorrows();
        })
        .catch((error) => {
          this.ErrorHandling(error);
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
    convertToTimestamp() {
      this.returnBookInfo.borrowTime = new Date(
        this.returnBookInfo.borrowTimeH
      ).getTime();
      this.returnBookInfo.returnTime = new Date(
        this.returnBookInfo.returnTimeH
      ).getTime();
    },
  },
};
</script>
