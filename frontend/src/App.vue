<template>
  <div>
    <div class="main" style="overflow-y: hidden">
      <el-container>
        <el-header class="title">
          <div
            style="
              margin-top: 12px;
              display: block;
              text-overflow: ellipsis;
              overflow: hidden;
              white-space: nowrap;
            "
          >
            <el-button
              type="primary"
              :icon="isCollapse ? 'Expand' : 'Fold'"
              style="color: white; margin-right: 20px; vertical-align: middle"
              @click="toggleSidebar"
            ></el-button>
            <img
              src="./components/icons/logo.png"
              style="margin-right: 20px; height: 40px; vertical-align: middle"
            />
            <span
              style="
                font-size: large;
                font-family: 'Microsoft YaHei';
                color: black;
                font-weight: bold;
              "
              >图书管理系统</span
            >
            <span
              style="
                margin-left: 40px;
                color: rgba(0, 0, 0, 0.2);
              "
              >浙江大学数据库系统课程项目</span
            >
          </div>
        </el-header>
        <el-container style="width: 100%">
          <el-aside
            class="aside"
            style="display: flex"
            :collapse="isCollapse"
            v-bind:style="{ width: isCollapse ? '0%' : '180px' }"
          >
            <el-menu
              active-text-color="#ffd04b"
              background-color="#0270c1"
              default-active="1"
              text-color="#fff"
              style="height: 100%; width: 100%"
              :router="true"
            >
              <el-menu-item index="book">
                <el-icon>
                  <Reading />
                </el-icon>
                <span>图书管理</span>
              </el-menu-item>
              <el-menu-item index="card">
                <el-icon>
                  <Postcard />
                </el-icon>
                <span>借书证管理</span>
              </el-menu-item>
              <el-menu-item index="borrow">
                <el-icon>
                  <Tickets />
                </el-icon>
                <span>借书记录查询</span>
              </el-menu-item>
            </el-menu>
          </el-aside>

          <el-main style="height: 100%; width: 100%">
            <el-scrollbar height="100%">
              <RouterView
                class="content"
                style="
                  height: 90vh;
                  max-height: 100%;
                  background-color: white;
                  color: black;
                "
              />
            </el-scrollbar>
          </el-main>
        </el-container>
      </el-container>
    </div>
  </div>
</template>

<script></script>

<script>
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
import { RouterView } from "vue-router";

export default {
  data() {
    return {
      isCollapse: true,
    };
  },
  methods: {
    toggleSidebar() {
      this.isCollapse = !this.isCollapse;
    },
  },
};
</script>

<style scoped>
#app {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background-color: #dcdcdc;
  width: 100vw;
  height: 100vh;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  width: 100%;
  min-height: 100%;
  height: auto;
  background-color: #dcdcdc;
}

.title {
  background-color: #ffffff;
  height: 60px;
}

.aside {
  min-height: calc(100vh - 60px);
  width: 180px;
  background-color: red;
}
</style>
