package com.example.domain

data class ChapterCurriculum(
    val number: Int,
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val keyFormulas: List<String>,
    val coreTopics: List<String>,
    val sampleSocraticPrompt: String,
    val sampleSolverPrompt: String,
    val sampleOralQuestion: String
)

object PoliticalEconomyCurriculum {
    val chapters: List<ChapterCurriculum> = listOf(
        ChapterCurriculum(
            number = 1,
            title = "Chương 1: Đối tượng, phương pháp nghiên cứu và chức năng của Kinh tế chính trị Mác - Lênin",
            shortTitle = "Đối tượng & Phương pháp",
            subtitle = "Sự hình thành, đối tượng quan hệ sản xuất và phương pháp trừu tượng hóa khoa học",
            keyFormulas = listOf("QHSX ↔ LLSX", "Trừu tượng hóa khoa học"),
            coreTopics = listOf(
                "Quá trình phát triển từ chủ nghĩa trọng thương, trọng nông đến kinh tế chính trị cổ điển",
                "Đối tượng nghiên cứu: các quan hệ xã hội của sản xuất và trao đổi",
                "Phương pháp trừu tượng hóa khoa học & kết hợp logic với lịch sử",
                "4 chức năng: Nhận thức, Thực tiễn, Tư tưởng, Phương pháp luận"
            ),
            sampleSocraticPrompt = "Hãy hướng dẫn tôi hiểu tại sao phương pháp trừu tượng hóa khoa học lại quan trọng nhất trong Kinh tế chính trị Mác - Lênin?",
            sampleSolverPrompt = "Phân biệt đối tượng nghiên cứu của Kinh tế chính trị Mác - Lênin với kinh tế học vi mô/vĩ mô hiện đại.",
            sampleOralQuestion = "Trình bày đối tượng nghiên cứu của Kinh tế chính trị Mác - Lênin và giải thích vì sao phải đặt nó trong mối liên hệ biện chứng với lực lượng sản xuất và kiến trúc thượng tầng?"
        ),
        ChapterCurriculum(
            number = 2,
            title = "Chương 2: Hàng hóa, thị trường và vai trò của các chủ thể tham gia thị trường",
            shortTitle = "Hàng hóa & Thị trường",
            subtitle = "Hai thuộc tính của hàng hóa, tính hai mặt của lao động, tiền tệ và quy luật giá trị",
            keyFormulas = listOf("W = c + v + m", "T - H - T'", "M = (P × Q) / V"),
            coreTopics = listOf(
                "Điều kiện ra đời của sản xuất hàng hóa & hai thuộc tính: Giá trị sử dụng và Giá trị",
                "Tính hai mặt của lao động: Lao động cụ thể và Lao động trừu tượng",
                "Lượng giá trị hàng hóa & các nhân tố ảnh hưởng (năng suất, cường độ, phức tạp)",
                "Bản chất và 5 chức năng của tiền tệ",
                "Quy luật giá trị, quy luật cung - cầu, quy luật lưu thông tiền tệ, quy luật cạnh tranh"
            ),
            sampleSocraticPrompt = "Tôi hay nhầm giữa tăng năng suất lao động và tăng cường độ lao động tác động đến giá trị 1 đơn vị hàng hóa. Hãy hỏi gợi mở để tôi tự phân biệt.",
            sampleSolverPrompt = "Một xí nghiệp trong 8 giờ sản xuất được 16 sản phẩm có tổng giá trị 80 USD. Nếu năng suất lao động tăng 2 lần thì giá trị tổng sản phẩm và giá trị 1 sản phẩm thay đổi thế nào?",
            sampleOralQuestion = "Phân tích tính hai mặt của lao động sản xuất hàng hóa và chứng minh vì sao nói phát hiện này là chìa khóa để giải thích mọi hiện tượng của kinh tế hàng hóa?"
        ),
        ChapterCurriculum(
            number = 3,
            title = "Chương 3: Giá trị thặng dư trong nền kinh tế thị trường",
            shortTitle = "Giá trị thặng dư",
            subtitle = "Hàng hóa sức lao động, c và v, công thức m', M, tích lũy tư bản và các hình thái biểu hiện",
            keyFormulas = listOf("m' = (m / v) × 100%", "M = m' × V", "p' = [m / (c + v)] × 100%", "c / v"),
            coreTopics = listOf(
                "Công thức chung của tư bản T - H - T' và mâu thuẫn công thức chung",
                "Hai điều kiện để sức lao động trở thành hàng hóa & hai thuộc tính của nó",
                "Tư bản bất biến (c) và Tư bản khả biến (v); Tư bản cố định (c1) và Tư bản lưu động (c2 + v)",
                "Hai phương pháp sản xuất giá trị thặng dư: Tuyệt đối và Tương đối (siêu ngạch)",
                "Tuần hoàn, chu chuyển tư bản, tích lũy tư bản & quy luật chung của tích lũy",
                "Lợi nhuận, tỷ suất lợi nhuận bình quân, lợi tức và địa tô tư bản chủ nghĩa"
            ),
            sampleSocraticPrompt = "Giải thích bằng phương pháp Socratic vì sao nhà tư bản trả đúng giá trị sức lao động nhưng vẫn thu được giá trị thặng dư?",
            sampleSolverPrompt = "Tư bản ứng trước là 100.000 USD, cấu tạo hữu cơ c/v = 4/1, tỷ suất giá trị thặng dư m' = 150%. Tính tư bản bất biến c, tư bản khả biến v, khối lượng giá trị thặng dư M và tỷ suất lợi nhuận p'?",
            sampleOralQuestion = "Phân biệt giá trị thặng dư tuyệt đối, giá trị thặng dư tương đối và giá trị thặng dư siêu ngạch. Nêu ý nghĩa thực tiễn đối với doanh nghiệp hiện nay."
        ),
        ChapterCurriculum(
            number = 4,
            title = "Chương 4: Cạnh tranh và độc quyền trong nền kinh tế thị trường",
            shortTitle = "Cạnh tranh & Độc quyền",
            subtitle = "Nguyên nhân hình thành độc quyền, 5 đặc điểm kinh tế của Lênin và độc quyền nhà nước",
            keyFormulas = listOf("Giá cả ĐQ = k + P_ĐQ", "5 đặc điểm Lênin"),
            coreTopics = listOf(
                "Tích tụ và tập trung sản xuất dẫn đến độc quyền",
                "5 đặc điểm kinh tế cơ bản của độc quyền theo V.I. Lênin",
                "Tư bản tài chính và hệ thống tài phiệt, xuất khẩu tư bản",
                "Độc quyền nhà nước trong chủ nghĩa tư bản và tác động hai mặt",
                "Những biểu hiện mới của độc quyền trong bối cảnh kinh tế số"
            ),
            sampleSocraticPrompt = "Độc quyền sinh ra từ cạnh tranh tự do, vậy khi có độc quyền rồi thì cạnh tranh có biến mất không? Hãy dẫn dắt tôi phân tích.",
            sampleSolverPrompt = "Hệ thống hóa 5 đặc điểm kinh tế cơ bản của chủ nghĩa tư bản độc quyền theo V.I. Lênin và biểu hiện mới trong thời đại công nghệ số.",
            sampleOralQuestion = "Vì sao V.I. Lênin khẳng định độc quyền không thủ tiêu cạnh tranh mà còn làm cho cạnh tranh trở nên gay gắt và phức tạp hơn?"
        ),
        ChapterCurriculum(
            number = 5,
            title = "Chương 5: Kinh tế thị trường định hướng xã hội chủ nghĩa và các quan hệ lợi ích kinh tế ở Việt Nam",
            shortTitle = "KTTT định hướng XHCN",
            subtitle = "Đặc trưng kinh tế thị trường định hướng XHCN, hoàn thiện thể chế và hài hòa lợi ích",
            keyFormulas = listOf("Nhiều hình thức sở hữu", "Nhiều thành phần KT"),
            coreTopics = listOf(
                "Khái niệm và tính tất yếu khách quan của KTTT định hướng XHCN ở Việt Nam",
                "5 đặc trưng cơ bản về mục tiêu, sở hữu, quản lý, phân phối và gắn tăng trưởng với tiến bộ XH",
                "Hoàn thiện thể chế KTTT định hướng XHCN",
                "Quan hệ lợi ích kinh tế và vai trò điều tiết của Nhà nước trong bảo đảm hài hòa lợi ích"
            ),
            sampleSocraticPrompt = "Điểm khác biệt cốt lõi nào phân biệt Kinh tế thị trường định hướng XHCN ở Việt Nam với kinh tế thị trường tư bản chủ nghĩa?",
            sampleSolverPrompt = "Trình bày các hình thức phân phối thu nhập trong nền kinh tế thị trường định hướng XHCN ở Việt Nam và vai trò của từng hình thức.",
            sampleOralQuestion = "Phân tích mối quan hệ giữa tăng trưởng kinh tế với thực hiện tiến bộ và công bằng xã hội trong mô hình kinh tế thị trường định hướng XHCN ở Việt Nam."
        ),
        ChapterCurriculum(
            number = 6,
            title = "Chương 6: Công nghiệp hóa, hiện đại hóa và hội nhập kinh tế quốc tế của Việt Nam",
            shortTitle = "CNH-HĐH & Hội nhập",
            subtitle = "Cách mạng công nghiệp 4.0, mô hình CNH-HĐH và xây dựng nền kinh tế độc lập tự chủ",
            keyFormulas = listOf("CMCN 4.0", "Độc lập tự chủ + Hội nhập"),
            coreTopics = listOf(
                "Khái quát các cuộc cách mạng công nghiệp và tác động đến phát triển",
                "Tính tất yếu và nội dung CNH, HĐH ở Việt Nam gắn với chuyển đổi số và tăng trưởng xanh",
                "Tác động tích cực và thách thức của hội nhập kinh tế quốc tế",
                "Phương hướng xây dựng nền kinh tế độc lập, tự chủ gắn với hội nhập sâu rộng"
            ),
            sampleSocraticPrompt = "Làm thế nào để Việt Nam vừa hội nhập kinh tế quốc tế sâu rộng vừa giữ vững nền kinh tế độc lập, tự chủ?",
            sampleSolverPrompt = "Phân tích nội dung chuyển đổi cơ cấu kinh tế theo hướng hiện đại trong tiến trình CNH-HĐH gắn với Cách mạng công nghiệp 4.0 tại Việt Nam.",
            sampleOralQuestion = "Trình bày mối quan hệ biện chứng giữa xây dựng nền kinh tế độc lập, tự chủ và chủ động, tích cực hội nhập kinh tế quốc tế ở Việt Nam."
        )
    )

    fun byNumber(number: Int): ChapterCurriculum? = chapters.firstOrNull { it.number == number }
}
