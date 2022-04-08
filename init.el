(setq package-archives '(
			 ("gnu"   . "http://mirrors.tuna.tsinghua.edu.cn/elpa/gnu/")
			 ("melpa" . "http://mirrors.tuna.tsinghua.edu.cn/elpa/melpa/")
			 ("org" . "http://mirrors.tuna.tsinghua.edu.cn/elpa/org/")))
(package-initialize)

;; init file kbd
(defun open-init-file()
  (interactive)
  (find-file "c:/Users/chuan/AppData/Roaming/.emacs.d/init.el"))

(global-set-key (kbd "<f12>") 'open-init-file)

;; eval-buffer
(global-set-key (kbd "<f5>") 'eval-buffer)

;; org mode
(dolist (package '(
		   evil
		   prescient
		   ivy-prescient
		   company-prescient
		   company
		   counsel
		   org-roam
		   emacsql-sqlite3
		   org-roam-ui
		   pyim-basedict
		   pyim
		   magit
		   ))
  (unless (package-installed-p package)
    (package-install package))
  (require package))

;; evil
(evil-mode 1)

;; prescient
(ivy-prescient-mode t)
(company-prescient-mode t)

;; company
(global-company-mode t)


;; ivy
(ivy-mode 1)
(setq ivy-use-virtual-buffers t)
(setq ivy-count-format "(%d/%d) ")

(global-set-key (kbd "C-s") 'swiper-isearch)
(global-set-key (kbd "M-x") 'counsel-M-x)
(global-set-key (kbd "C-x C-f") 'counsel-find-file)
(global-set-key (kbd "M-y") 'counsel-yank-pop)
(global-set-key (kbd "<f1> f") 'counsel-describe-function)
(global-set-key (kbd "<f1> v") 'counsel-describe-variable)
(global-set-key (kbd "<f1> l") 'counsel-find-library)
(global-set-key (kbd "<f2> i") 'counsel-info-lookup-symbol)
(global-set-key (kbd "<f2> u") 'counsel-unicode-char)
(global-set-key (kbd "<f2> j") 'counsel-set-variable)
(global-set-key (kbd "C-x b") 'ivy-switch-buffer)
(global-set-key (kbd "C-c v") 'ivy-push-view)
(global-set-key (kbd "C-c V") 'ivy-pop-view)

(global-set-key (kbd "C-c c") 'counsel-compile)
(global-set-key (kbd "C-c g") 'counsel-git)
(global-set-key (kbd "C-c j") 'counsel-git-grep)
(global-set-key (kbd "C-c L") 'counsel-git-log)
(global-set-key (kbd "C-c k") 'counsel-rg)
(global-set-key (kbd "C-c m") 'counsel-linux-app)
(global-set-key (kbd "C-c n") 'counsel-fzf)
(global-set-key (kbd "C-x l") 'counsel-locate)
(global-set-key (kbd "C-c J") 'counsel-file-jump)
(global-set-key (kbd "C-S-o") 'counsel-rhythmbox)
(global-set-key (kbd "C-c w") 'counsel-wmctrl)

(global-set-key (kbd "C-c C-r") 'ivy-resume)
(global-set-key (kbd "C-c b") 'counsel-bookmark)
(global-set-key (kbd "C-c d") 'counsel-descbinds)
(global-set-key (kbd "C-c g") 'counsel-git)
(global-set-key (kbd "C-c o") 'counsel-outline)
(global-set-key (kbd "C-c t") 'counsel-load-theme)
(global-set-key (kbd "C-c F") 'counsel-org-file)


;; org-roam
;;(make-directory "d:/org-roam")
(setq org-roam-directory (file-truename "d:/org/roam/"))
(setq find-file-visit-truename t)
(setq org-roam-database-connector 'sqlite3)
;;(org-roam-db-autosync-mode)
(global-set-key (kbd "C->") 'org-roam-node-find)
;; (global-set-key (kbd "C-?") 'org-roam-dailies-capture-today)
(global-set-key (kbd "C-'") 'org-roam-dailies-goto-today)


(add-to-list 'display-buffer-alist
             '("\\*org-roam\\*"
               (display-buffer-in-direction)
               (direction . right)
               (window-width . 0.33)
               (window-height . fit-window-to-buffer)))
;;(add-to-list 'display-buffer-alist
;;             '("\\*org-roam\\*"
;;               (display-buffer-in-side-window)
;;               (side . right)
;;               (slot . 0)
;;               (window-width . 0.33)
;;               (window-parameters . ((no-other-window . t)
;;                                     (no-delete-other-windows . t)))))


;; pyim
;; Basedict
(pyim-basedict-enable)
(setq default-input-method "pyim")

;; 金手指设置，可以将光标处的编码，比如：拼音字符串，转换为中文。
(global-set-key (kbd "M-j") 'pyim-convert-string-at-point)

;; 按 "C-<return>" 将光标前的 regexp 转换为可以搜索中文的 regexp.
(define-key minibuffer-local-map (kbd "C-<return>") 'pyim-cregexp-convert-at-point)

;; 我拼
(pyim-default-scheme 'microsoft-shuangpin)
;; (pyim-default-scheme 'wubi)
;; (pyim-default-scheme 'cangjie)

;; pyim 探针设置
;; 设置 pyim 探针设置，这是 pyim 高级功能设置，可以实现 *无痛* 中英文切换 :-)
;; 我自己使用的中英文动态切换规则是：
;; 1. 光标只有在注释里面时，才可以输入中文。
;; 2. 光标前是汉字字符时，才能输入中文。
;; 3. 使用 M-j 快捷键，强制将光标前的拼音字符串转换为中文。
(setq-default pyim-english-input-switch-functions
              '(pyim-probe-dynamic-english
                pyim-probe-isearch-mode
                ;;pyim-probe-program-mode
                pyim-probe-org-structure-template))

(setq-default pyim-punctuation-half-width-functions
              '(pyim-probe-punctuation-line-beginning
                pyim-probe-punctuation-after-punctuation))

;; 开启代码搜索中文功能（比如拼音，五笔码等）
(pyim-isearch-mode 1)

;; 显示5个候选词。
(setq pyim-page-length 5)


(custom-set-variables
 ;; custom-set-variables was added by Custom.
 ;; If you edit it by hand, you could mess it up, so be careful.
 ;; Your init file should contain only one such instance.
 ;; If there is more than one, they won't work right.
 '(current-language-environment "Chinese-GBK")
 '(custom-enabled-themes '(leuven))
 '(display-line-numbers-type nil)
 '(package-selected-packages
   '(org-roam-ui company-prescient company ivy-prescient prescient emacsql-sqlite3 counsel org-roam evil))
 '(scroll-bar-mode nil)
 '(show-paren-mode t)
 '(tool-bar-mode nil)
 '(tooltip-mode nil)
 '(visible-bell 0)
 '(visual-line-mode t))
(custom-set-faces
 ;; custom-set-faces was added by Custom.
 ;; If you edit it by hand, you could mess it up, so be careful.
 ;; Your init file should contain only one such instance.
 ;; If there is more than one, they won't work right.
 '(default ((t (:family "霞鹜文楷" :foundry "outline" :slant normal :weight normal :height 120 :width normal)))))


