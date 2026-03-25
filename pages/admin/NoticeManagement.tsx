import React, { useState } from "react";
import { Plus, Search, Edit2, Trash2, AlertCircle } from "lucide-react";
import { MOCK_NOTICES } from "@/services/mockData";
import { Notice, NoticeCategory } from "@/types";

export const NoticeManagement: React.FC = () => {
  const [notices, setNotices] = useState<Notice[]>(MOCK_NOTICES);
  const [searchTerm, setSearchTerm] = useState("");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [editingNotice, setEditingNotice] = useState<Notice | null>(null);
  const [noticeToDelete, setNoticeToDelete] = useState<string | null>(null);

  // Form States
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState<NoticeCategory>("업데이트");
  const [description, setDescription] = useState("");
  const [content, setContent] = useState("");
  const [isImportant, setIsImportant] = useState(false);

  const filteredNotices = notices.filter(
    (n) =>
      n.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      n.content.toLowerCase().includes(searchTerm.toLowerCase()),
  );

  const handleOpenModal = (notice?: Notice) => {
    if (notice) {
      setEditingNotice(notice);
      setTitle(notice.title);
      setCategory(notice.category);
      setContent(notice.content);
      setIsImportant(notice.isImportant);
    } else {
      setEditingNotice(null);
      setTitle("");
      setCategory("업데이트");
      setContent("");
      setIsImportant(false);
    }
    setIsModalOpen(true);
  };

  const handleSave = () => {
    if (editingNotice) {
      setNotices((prev) =>
        prev.map((n) =>
          n.id === editingNotice.id
            ? {
                ...n,
                title,
                category,
                description: "",
                content,
                isImportant,
              }
            : n,
        ),
      );
    } else {
      const newNotice: Notice = {
        id: `notice_${Date.now()}`,
        title,
        category,
        description: "",
        content,
        isImportant,
        createdAt: new Date().toISOString(),
      };
      setNotices([newNotice, ...notices]);
    }
    setIsModalOpen(false);
  };

  const handleDelete = (id: string) => {
    setNoticeToDelete(id);
    setIsDeleteModalOpen(true);
  };

  const confirmDelete = () => {
    if (noticeToDelete) {
      setNotices((prev) => prev.filter((n) => n.id !== noticeToDelete));
      setNoticeToDelete(null);
      setIsDeleteModalOpen(false);
    }
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-gray-900 tracking-tight">
            공지사항 관리
          </h1>
          <p className="text-gray-500 mt-1 text-[11px] font-medium">
            서비스 공지사항을 등록하고 관리합니다.
          </p>
        </div>
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          <div className="relative w-full sm:w-64 flex items-center h-10">
            <div className="absolute left-3 top-0 bottom-0 flex items-center pointer-events-none">
              <Search className="text-gray-400 w-4 h-4" />
            </div>
            <input
              type="text"
              placeholder="제목 또는 내용 검색"
              className="w-full pl-10 pr-4 h-full bg-white border border-gray-200 rounded-none shadow-sm focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] focus:border-transparent font-bold text-xs"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <button
            onClick={() => handleOpenModal()}
            className="bg-[#FF5A5A] text-white px-6 py-2 rounded-none font-black hover:bg-[#E04848] transition-all flex items-center justify-center shadow-lg shadow-red-900/10 active:scale-95 shrink-0 text-sm"
          >
            <Plus className="w-4 h-4 mr-2" /> 새 공지 등록
          </button>
        </div>
      </header>

      {/* Notice List */}
      <div className="grid grid-cols-1 gap-4">
        {filteredNotices.map((notice) => (
          <div
            key={notice.id}
            className="bg-white p-5 rounded-none shadow-sm border border-gray-100 flex items-start justify-between group hover:shadow-md transition-all"
          >
            <div className="flex-1">
              <div className="flex items-center space-x-2 mb-2">
                <span
                  className={`px-2 py-0.5 rounded-none text-[10px] font-black ${
                    notice.category === "점검"
                      ? "bg-orange-100 text-orange-700"
                      : notice.category === "업데이트"
                        ? "bg-blue-100 text-blue-700"
                        : notice.category === "이벤트"
                          ? "bg-purple-100 text-purple-700"
                          : "bg-gray-100 text-gray-700"
                  }`}
                >
                  {notice.category}
                </span>
                {notice.isImportant && (
                  <span className="flex items-center text-[10px] font-black text-red-600 bg-red-50 px-2 py-0.5 rounded-none">
                    중요
                  </span>
                )}
                <span className="text-[10px] font-medium text-gray-400">
                  {notice.createdAt.split("T")[0]}
                </span>
              </div>
              <h3 className="text-base font-black text-gray-900 mb-1">
                {notice.title}
              </h3>
            </div>

            <div className="flex items-center space-x-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                onClick={() => handleOpenModal(notice)}
                className="p-2 hover:bg-gray-100 text-gray-600 rounded-none transition-colors"
              >
                <Edit2 className="w-4 h-4" />
              </button>
              <button
                onClick={() => handleDelete(notice.id)}
                className="p-2 hover:bg-red-100 text-red-600 rounded-none transition-colors"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Editor Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-6 bg-black/60 backdrop-blur-sm">
          <div className="bg-white w-full max-w-2xl rounded-none p-8 shadow-2xl animate-in fade-in zoom-in duration-300 max-h-[90vh] overflow-y-auto">
            <h2 className="text-2xl font-black text-gray-900 mb-6 tracking-tight">
              {editingNotice ? "공지사항 수정" : "새 공지사항 등록"}
            </h2>

            <div className="space-y-4 mb-8">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-black text-gray-700 mb-2">
                    카테고리
                  </label>
                  <select
                    className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-bold appearance-none text-sm"
                    value={category}
                    onChange={(e) =>
                      setCategory(e.target.value as NoticeCategory)
                    }
                  >
                    <option value="업데이트">업데이트</option>
                    <option value="이벤트">이벤트</option>
                    <option value="점검">점검</option>
                    <option value="정책">정책</option>
                  </select>
                </div>
                <div className="flex items-end pb-3">
                  <label className="flex items-center space-x-2 cursor-pointer group">
                    <input
                      type="checkbox"
                      className="w-5 h-5 rounded-none border-gray-300 text-[#FF5A5A] focus:ring-[#FF5A5A] cursor-pointer"
                      checked={isImportant}
                      onChange={(e) => setIsImportant(e.target.checked)}
                    />
                    <span className="text-sm font-black text-gray-700 group-hover:text-[#FF5A5A] transition-colors">
                      중요 공지로 설정
                    </span>
                  </label>
                </div>
              </div>

              <div>
                <label className="block text-xs font-black text-gray-700 mb-2">
                  제목
                </label>
                <input
                  type="text"
                  placeholder="공지사항 제목을 입력하세요"
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-bold text-sm"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                />
              </div>

              <div>
                <label className="block text-xs font-black text-gray-700 mb-2">
                  상세 내용
                </label>
                <textarea
                  rows={6}
                  placeholder="공지사항 상세 내용을 입력하세요"
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-medium resize-none text-sm"
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                />
              </div>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => setIsModalOpen(false)}
                className="flex-1 py-3 rounded-none font-black text-gray-500 bg-gray-100 hover:bg-gray-200 transition-all text-sm"
              >
                취소
              </button>
              <button
                onClick={handleSave}
                className="flex-1 py-3 rounded-none font-black text-white bg-[#FF5A5A] hover:bg-[#E04848] transition-all shadow-lg shadow-red-500/20 text-sm"
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
      {/* Delete Confirmation Modal */}
      {isDeleteModalOpen && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-6 bg-black/60 backdrop-blur-sm">
          <div className="bg-white w-full max-w-sm rounded-none p-8 shadow-2xl animate-in fade-in zoom-in duration-300">
            <div className="w-16 h-16 bg-red-50 rounded-none flex items-center justify-center mb-4 mx-auto">
              <Trash2 className="w-8 h-8 text-red-500" />
            </div>
            <h2 className="text-xl font-black text-gray-900 mb-2 text-center tracking-tight">
              정말 삭제하시겠습니까?
            </h2>
            <p className="text-gray-500 font-medium text-center mb-8 text-sm">
              삭제된 공지사항은 복구할 수 없습니다.
              <br />
              신중하게 결정해주세요.
            </p>

            <div className="flex gap-3">
              <button
                onClick={() => setIsDeleteModalOpen(false)}
                className="flex-1 py-3 rounded-none font-black text-gray-500 bg-gray-100 hover:bg-gray-200 transition-all text-sm"
              >
                취소
              </button>
              <button
                onClick={confirmDelete}
                className="flex-1 py-3 rounded-none font-black text-white bg-[#FF5A5A] hover:bg-[#E04848] transition-all shadow-lg shadow-red-500/20 text-sm"
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
