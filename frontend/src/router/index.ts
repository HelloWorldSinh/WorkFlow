import { createRouter, createWebHistory } from 'vue-router'
import WorkflowListView from '@/views/WorkflowListView.vue'
import LoginView from '@/views/LoginView.vue'
import WorkflowEditorView from '@/views/WorkflowEditorView.vue'
import FormsView from '@/views/FormsView.vue'
import TicketListView from '@/views/TicketListView.vue'
import TicketDetailView from '@/views/TicketDetailView.vue'
import MyTasksView from '@/views/MyTasksView.vue'

import WorkflowFormMatchView from '@/views/WorkflowFormMatchView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { layout: 'auth' },
    },
    {
      path: '/',
      name: 'workflows',
      component: WorkflowListView,
    },
    {
      path: '/forms',
      name: 'forms',
      component: FormsView,
    },
    {
      path: '/workflows/:id/editor',
      name: 'workflow-editor',
      component: WorkflowEditorView,
      meta: { layout: 'editor' },
    },
    {
      path: '/workflows/:id/edit',
      redirect: (to) => `/workflows/${to.params.id}/editor`,
    },
    {
      path: '/workflows/:id/match-form',
      name: 'workflow-match-form',
      component: WorkflowFormMatchView,
    },
    {
      path: '/match-form',
      name: 'match-form',
      component: WorkflowFormMatchView,
    },
    {
      path: '/editor',
      name: 'editor-new',
      component: WorkflowEditorView,
      meta: { layout: 'editor' },
    },
    {
      path: '/tickets',
      name: 'tickets',
      component: TicketListView,
    },
    {
      path: '/tickets/:id',
      name: 'ticket-detail',
      component: TicketDetailView,
    },
    {
      path: '/my-tasks',
      name: 'my-tasks',
      component: MyTasksView,
    },
  ],
})

export default router
