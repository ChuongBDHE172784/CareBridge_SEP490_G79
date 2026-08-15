const ExcelJS = require('exceljs');
const fs = require('fs');
const path = require('path');

async function buildFilledTemplate() {
  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'CareBridge System';
  workbook.created = new Date();
  workbook.modified = new Date();

  const primaryHeaderFill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'FF1E3A8A' }, // Deep Blue
  };
  const secondaryHeaderFill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'FF0D9488' }, // Teal
  };
  const headerFont = {
    name: 'Calibri',
    size: 11,
    bold: true,
    color: { argb: 'FFFFFFFF' },
  };
  const borderThin = {
    top: { style: 'thin', color: { argb: 'FFCBD5E1' } },
    left: { style: 'thin', color: { argb: 'FFCBD5E1' } },
    bottom: { style: 'thin', color: { argb: 'FFCBD5E1' } },
    right: { style: 'thin', color: { argb: 'FFCBD5E1' } },
  };

  // ==========================================
  // SHEET 1: Checklists
  // ==========================================
  const sheet1 = workbook.addWorksheet('Checklists', {
    views: [{ state: 'frozen', ySplit: 1 }],
  });

  sheet1.columns = [
    { header: 'checklist_code', key: 'checklist_code', width: 22 },
    { header: 'name', key: 'name', width: 38 },
    { header: 'description', key: 'description', width: 50 },
    { header: 'stage', key: 'stage', width: 22 },
    { header: 'template_type', key: 'template_type', width: 18 },
    { header: 'window_start', key: 'window_start', width: 15 },
    { header: 'window_end', key: 'window_end', width: 15 },
    { header: 'end_at_stage_exit', key: 'end_at_stage_exit', width: 20 },
    { header: 'display_order', key: 'display_order', width: 16 },
    { header: 'repeat_mode', key: 'repeat_mode', width: 18 },
  ];

  const headerRow1 = sheet1.getRow(1);
  headerRow1.height = 28;
  headerRow1.eachCell((cell) => {
    cell.fill = primaryHeaderFill;
    cell.font = headerFont;
    cell.alignment = { vertical: 'middle', horizontal: 'center' };
    cell.border = borderThin;
  });

  const rootRows = [
    // Giai đoạn 1: MUỐN MANG THAI (PRE_PREGNANCY)
    {
      checklist_code: 'PRE_PREG_01',
      name: 'Lập kế hoạch mang thai',
      description: 'Tư vấn và chuẩn bị sớm trước khi có thai',
      stage: 'PRE_PREGNANCY',
      template_type: 'MANDATORY',
      window_start: '',
      window_end: '',
      end_at_stage_exit: 'FALSE',
      display_order: 1,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'PRE_PREG_02',
      name: 'Đánh giá sức khỏe & yếu tố nguy cơ',
      description: 'Xem xét tiền sử mang thai, bệnh lý bản thân, gia đình và môi trường sống',
      stage: 'PRE_PREGNANCY',
      template_type: 'MANDATORY',
      window_start: '',
      window_end: '',
      end_at_stage_exit: 'FALSE',
      display_order: 2,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'PRE_PREG_03',
      name: 'Khám, xét nghiệm & điều trị trước thai kỳ',
      description: 'Khám phụ khoa, kiểm soát bệnh mạn tính và rà soát thuốc đang dùng',
      stage: 'PRE_PREGNANCY',
      template_type: 'MANDATORY',
      window_start: '',
      window_end: '',
      end_at_stage_exit: 'FALSE',
      display_order: 3,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'PRE_PREG_04',
      name: 'Điều chỉnh dinh dưỡng và lối sống',
      description: 'Chế độ ăn đa dạng, duy trì cân nặng hợp lý và vận động thể lực',
      stage: 'PRE_PREGNANCY',
      template_type: 'MANDATORY',
      window_start: '',
      window_end: '',
      end_at_stage_exit: 'FALSE',
      display_order: 4,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'PRE_PREG_05',
      name: 'Bổ sung vi chất & hoàn thành tiêm chủng',
      description: 'Bổ sung sắt, axit folic và tiêm các vắc-xin cần thiết trước mang thai',
      stage: 'PRE_PREGNANCY',
      template_type: 'MANDATORY',
      window_start: '',
      window_end: '',
      end_at_stage_exit: 'FALSE',
      display_order: 5,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'PRE_PREG_06',
      name: 'Chuẩn bị sẵn sàng để thụ thai',
      description: 'Theo dõi chu kỳ kinh nguyệt và trang bị kiến thức tiền sản',
      stage: 'PRE_PREGNANCY',
      template_type: 'MANDATORY',
      window_start: '',
      window_end: '',
      end_at_stage_exit: 'FALSE',
      display_order: 6,
      repeat_mode: 'NONE',
    },

    // Giai đoạn 2: ĐANG MANG THAI (PREGNANCY)
    // 2.1 Không lặp (Theo các mốc tuần)
    {
      checklist_code: 'PREG_ONCE_01',
      name: 'Khám và xét nghiệm 20 tuần đầu',
      description: 'Khám thai lần đầu, xét nghiệm máu, sàng lọc dị tật và bệnh truyền nhiễm',
      stage: 'PREGNANCY',
      template_type: 'MANDATORY',
      window_start: 1,
      window_end: 20,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'PREG_ONCE_02',
      name: 'Theo dõi và siêu âm tuần 21 - 25',
      description: 'Thực hiện siêu âm hình thái học trước tuần 24 và hoàn thành xét nghiệm còn thiếu',
      stage: 'PREGNANCY',
      template_type: 'MANDATORY',
      window_start: 21,
      window_end: 25,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'PREG_ONCE_03',
      name: 'Sàng lọc đái tháo đường & Rh tuần 26 - 29',
      description: 'Xét nghiệm đường huyết thai kỳ và tiêm Anti-D nếu mẹ có Rh âm',
      stage: 'PREGNANCY',
      template_type: 'MANDATORY',
      window_start: 26,
      window_end: 29,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'PREG_ONCE_04',
      name: 'Kế hoạch sinh & chuẩn bị tuần 30 - 33',
      description: 'Tư vấn kế hoạch sinh, chọn nơi sinh và phương án cấp cứu',
      stage: 'PREGNANCY',
      template_type: 'MANDATORY',
      window_start: 30,
      window_end: 33,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'PREG_ONCE_05',
      name: 'Sàng lọc GBS & nuôi con sữa mẹ tuần 36 - 37',
      description: 'Sàng lọc liên cầu khuẩn nhóm B (GBS) và tư vấn nuôi con bằng sữa mẹ',
      stage: 'PREGNANCY',
      template_type: 'MANDATORY',
      window_start: 36,
      window_end: 37,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'PREG_ONCE_06',
      name: 'Chuẩn bị chuyển dạ tuần 38 - 39',
      description: 'Xác nhận nơi sinh, phương tiện di chuyển và nhận biết dấu hiệu chuyển dạ',
      stage: 'PREGNANCY',
      template_type: 'MANDATORY',
      window_start: 38,
      window_end: 39,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'PREG_ONCE_07',
      name: 'Chờ sinh & theo dõi từ tuần 40',
      description: 'Rà soát lần cuối kế hoạch sinh và kế hoạch theo dõi y tế nếu quá ngày dự sinh',
      stage: 'PREGNANCY',
      template_type: 'MANDATORY',
      window_start: 40,
      window_end: 42,
      end_at_stage_exit: 'TRUE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    // 2.2 Định kỳ hàng tuần
    {
      checklist_code: 'PREG_WEEKLY_01',
      name: 'Theo dõi chỉ số mẹ bầu hàng tuần',
      description: 'Đo huyết áp, cân nặng và cập nhật BMI hàng tuần trong suốt thai kỳ',
      stage: 'PREGNANCY',
      template_type: 'MANDATORY',
      window_start: 1,
      window_end: 42,
      end_at_stage_exit: 'TRUE',
      display_order: 0,
      repeat_mode: 'WEEKLY',
    },
    {
      checklist_code: 'PREG_WEEKLY_02',
      name: 'Sàng lọc tiền sản giật hàng tuần (từ tuần 21)',
      description: 'Kiểm tra protein niệu để sàng lọc nguy cơ tiền sản giật từ tuần 21',
      stage: 'PREGNANCY',
      template_type: 'MANDATORY',
      window_start: 21,
      window_end: 42,
      end_at_stage_exit: 'TRUE',
      display_order: 0,
      repeat_mode: 'WEEKLY',
    },
    {
      checklist_code: 'PREG_WEEKLY_03',
      name: 'Đếm cử động thai hàng tuần (từ tuần 30)',
      description: 'Theo dõi và ghi nhận cử động của thai nhi từ tuần 30',
      stage: 'PREGNANCY',
      template_type: 'MANDATORY',
      window_start: 30,
      window_end: 42,
      end_at_stage_exit: 'TRUE',
      display_order: 0,
      repeat_mode: 'WEEKLY',
    },
    // 2.3 Định kỳ hàng ngày
    {
      checklist_code: 'PREG_DAILY_01',
      name: 'Uống Axit Folic hàng ngày (20 tuần đầu)',
      description: 'Bổ sung Axit Folic 400mcg/ngày trong 20 tuần đầu thai kỳ',
      stage: 'PREGNANCY',
      template_type: 'MANDATORY',
      window_start: 1,
      window_end: 20,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'DAILY',
    },
    {
      checklist_code: 'PREG_DAILY_02',
      name: 'Uống Axit Folic hàng ngày (từ tuần 21)',
      description: 'Bổ sung Axit Folic 600mcg/ngày từ tuần 21 đến khi sinh',
      stage: 'PREGNANCY',
      template_type: 'MANDATORY',
      window_start: 21,
      window_end: 42,
      end_at_stage_exit: 'TRUE',
      display_order: 0,
      repeat_mode: 'DAILY',
    },

    // Giai đoạn 3: HẬU SẢN (POSTPARTUM)
    {
      checklist_code: 'POST_WEEK_01',
      name: 'Chăm sóc mẹ sau sinh — Tuần 1',
      description: 'Sàng lọc trầm cảm, theo dõi hồi phục vết may/mổ và tư vấn tránh thai',
      stage: 'POSTPARTUM',
      template_type: 'MANDATORY',
      window_start: 1,
      window_end: 1,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'POST_WEEK_02',
      name: 'Chăm sóc mẹ sau sinh — Tuần 2',
      description: 'Đánh giá dấu hiệu sinh tồn, co hồi tử cung, kiểm tra nhiễm trùng và dinh dưỡng',
      stage: 'POSTPARTUM',
      template_type: 'MANDATORY',
      window_start: 2,
      window_end: 2,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'POST_WEEK_06',
      name: 'Khám kiểm tra sau sinh — Tuần 6',
      description: 'Khám sức khỏe toàn diện, đánh giá tâm thần và chăm sóc sức khỏe dài hạn',
      stage: 'POSTPARTUM',
      template_type: 'MANDATORY',
      window_start: 6,
      window_end: 6,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },

    // Giai đoạn 4: CHĂM CON (BABY_CARE)
    {
      checklist_code: 'BABY_0_28D',
      name: 'Chăm sóc trẻ sơ sinh 0–28 ngày',
      description: 'Khám sơ sinh, bú mẹ, giữ ấm, tiêm chủng viêm gan B/BCG và theo dõi rốn/vàng da',
      stage: 'BABY_CARE',
      template_type: 'MANDATORY',
      window_start: 1,
      window_end: 4,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'BABY_1_2M',
      name: 'Chăm sóc trẻ 1–<2 tháng',
      description: 'Khám mốc 6 tuần, duy trì bú mẹ hoàn toàn và chuẩn bị tiêm chủng mốc 2 tháng',
      stage: 'BABY_CARE',
      template_type: 'MANDATORY',
      window_start: 5,
      window_end: 8,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'BABY_2_3M',
      name: 'Chăm sóc trẻ 2–3 tháng',
      description: 'Khám sức khỏe, tiêm chủng vắc xin phối hợp liều 1/Rota/bại liệt và theo dõi tăng trưởng',
      stage: 'BABY_CARE',
      template_type: 'MANDATORY',
      window_start: 9,
      window_end: 13,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'BABY_4_6M',
      name: 'Chăm sóc trẻ 4–6 tháng',
      description: 'Tiêm chủng cơ bản liều tiếp theo, bú mẹ hoàn toàn và chuẩn bị ăn dặm mốc 6 tháng',
      stage: 'BABY_CARE',
      template_type: 'MANDATORY',
      window_start: 14,
      window_end: 26,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'BABY_7_9M',
      name: 'Chăm sóc trẻ 7–9 tháng',
      description: 'Khám sức khỏe, ăn bổ sung 2-3 bữa/ngày, tiêm sởi/IPV2 và chuyển kết cấu thức ăn',
      stage: 'BABY_CARE',
      template_type: 'MANDATORY',
      window_start: 27,
      window_end: 39,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'BABY_10_12M',
      name: 'Chăm sóc trẻ 10–12 tháng',
      description: 'Khám sức khỏe, rà soát lịch tiêm chủng và chuẩn bị kiểm tra toàn diện mốc 12 tháng',
      stage: 'BABY_CARE',
      template_type: 'MANDATORY',
      window_start: 40,
      window_end: 52,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'BABY_12M',
      name: 'Mốc 12 tháng (1 tuổi)',
      description: 'Khám mốc 1 tuổi, tiêm Viêm não Nhật Bản B liều 1 và dinh dưỡng sau 1 tuổi',
      stage: 'BABY_CARE',
      template_type: 'MANDATORY',
      window_start: 52,
      window_end: 52,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'BABY_13_18M',
      name: 'Chăm sóc trẻ 13–18 tháng',
      description: 'Khám sức khỏe, chăm sóc răng miệng, phát triển ngôn ngữ và tiêm nhắc mốc 18 tháng',
      stage: 'BABY_CARE',
      template_type: 'MANDATORY',
      window_start: 52,
      window_end: 52,
      end_at_stage_exit: 'TRUE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'BABY_19_24M',
      name: 'Chăm sóc trẻ 19–24 tháng',
      description: 'Khám sức khỏe, tiêm Viêm não Nhật Bản B liều 3 và chuẩn bị kiểm tra mốc 24 tháng',
      stage: 'BABY_CARE',
      template_type: 'MANDATORY',
      window_start: 52,
      window_end: 52,
      end_at_stage_exit: 'TRUE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'BABY_SAFETY_0_24M',
      name: 'Dấu hiệu nguy hiểm cần cấp cứu (0–24 tháng)',
      description: 'Nhận biết các dấu hiệu nguy kịch để đưa trẻ đi cấp cứu/khám ngay lập tức',
      stage: 'BABY_CARE',
      template_type: 'MANDATORY',
      window_start: 1,
      window_end: 52,
      end_at_stage_exit: 'TRUE',
      display_order: 0,
      repeat_mode: 'NONE',
    },
  ];

  rootRows.forEach((r) => {
    const row = sheet1.addRow(r);
    row.height = 24;
    row.eachCell((cell, colNumber) => {
      cell.border = borderThin;
      cell.font = { name: 'Calibri', size: 10 };
      if ([1, 4, 5, 6, 7, 8, 9, 10].includes(colNumber)) {
        cell.alignment = { vertical: 'middle', horizontal: 'center' };
      } else {
        cell.alignment = { vertical: 'middle', horizontal: 'left' };
      }
    });
  });

  sheet1.autoFilter = `A1:J${rootRows.length + 1}`;

  // Data validations for Checklists
  for (let rowIdx = 2; rowIdx <= 100; rowIdx++) {
    sheet1.getCell(`D${rowIdx}`).dataValidation = {
      type: 'list',
      allowBlank: true,
      formulae: ['"PRE_PREGNANCY,PREGNANCY,POSTPARTUM,BABY_CARE"'],
    };
    sheet1.getCell(`E${rowIdx}`).dataValidation = {
      type: 'list',
      allowBlank: true,
      formulae: ['"MANDATORY,OPTIONAL"'],
    };
    sheet1.getCell(`H${rowIdx}`).dataValidation = {
      type: 'list',
      allowBlank: true,
      formulae: ['"TRUE,FALSE"'],
    };
    sheet1.getCell(`J${rowIdx}`).dataValidation = {
      type: 'list',
      allowBlank: true,
      formulae: ['"NONE,WEEKLY,DAILY"'],
    };
  }

  // ==========================================
  // SHEET 2: Checklist_Items
  // ==========================================
  const sheet2 = workbook.addWorksheet('Checklist_Items', {
    views: [{ state: 'frozen', ySplit: 1 }],
  });

  sheet2.columns = [
    { header: 'checklist_code', key: 'checklist_code', width: 22 },
    { header: 'order', key: 'order', width: 10 },
    { header: 'item_text', key: 'item_text', width: 44 },
    { header: 'description', key: 'description', width: 55 },
    { header: 'is_required', key: 'is_required', width: 16 },
    { header: 'support_function', key: 'support_function', width: 28 },
    { header: 'source_url', key: 'source_url', width: 38 },
  ];

  const headerRow2 = sheet2.getRow(1);
  headerRow2.height = 28;
  headerRow2.eachCell((cell) => {
    cell.fill = secondaryHeaderFill;
    cell.font = headerFont;
    cell.alignment = { vertical: 'middle', horizontal: 'center' };
    cell.border = borderThin;
  });

  const itemRows = [
    // Items for PRE_PREG_01
    { checklist_code: 'PRE_PREG_01', order: 1, item_text: 'Tư vấn và chuẩn bị trước khi mang thai', description: 'Đi tư vấn, chăm sóc trước khi có thai; nên bắt đầu chuẩn bị sớm.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'PRE_PREG_01', order: 2, item_text: 'Sử dụng biện pháp tránh thai phù hợp', description: 'Nếu chưa sẵn sàng mang thai, sử dụng biện pháp tránh thai phù hợp.', is_required: 'FALSE', support_function: 'CONTENT_LIBRARY', source_url: '' },

    // Items for PRE_PREG_02
    { checklist_code: 'PRE_PREG_02', order: 1, item_text: 'Xem xét tiền sử mang thai và sinh con trước', description: 'Xem xét đầy đủ tiền sử mang thai và sinh con trước đây.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'PRE_PREG_02', order: 2, item_text: 'Xem xét tiền sử bệnh bản thân và gia đình', description: 'Xem xét tiền sử bệnh của bản thân và gia đình.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'PRE_PREG_02', order: 3, item_text: 'Đánh giá nguy cơ bệnh mạn tính và di truyền', description: 'Đánh giá nguy cơ bệnh mạn tính, bệnh di truyền, bệnh lây truyền qua đường tình dục và các nhiễm khuẩn.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'PRE_PREG_02', order: 4, item_text: 'Rà soát yếu tố nghề nghiệp và môi trường', description: 'Xem xét thuốc đã sử dụng, nghề nghiệp, môi trường sống, làm việc và tiếp xúc hóa chất độc hại.', is_required: 'FALSE', support_function: 'HEALTH_RECORDS', source_url: '' },

    // Items for PRE_PREG_03
    { checklist_code: 'PRE_PREG_03', order: 1, item_text: 'Khám sức khỏe và khám phụ khoa định kỳ', description: 'Khám sức khỏe và khám phụ khoa định kỳ mỗi năm.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'PRE_PREG_03', order: 2, item_text: 'Điều trị bệnh phụ khoa và nhiễm khuẩn (nếu có)', description: 'Điều trị bệnh phụ khoa, nhiễm khuẩn đường sinh sản và bệnh lây truyền qua đường tình dục nếu có.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'PRE_PREG_03', order: 3, item_text: 'Kiểm soát bệnh mạn tính tiền thai kỳ', description: 'Kiểm soát tăng huyết áp, bệnh tim mạch, đái tháo đường và các bệnh mạn tính nếu có.', is_required: 'TRUE', support_function: 'MATERNAL_HEALTH_METRICS', source_url: '' },
    { checklist_code: 'PRE_PREG_03', order: 4, item_text: 'Rà soát thuốc và thực phẩm chức năng đang dùng', description: 'Rà soát thuốc đang dùng; thông báo đầy đủ thuốc, vitamin và thực phẩm chức năng để được đánh giá.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },

    // Items for PRE_PREG_04
    { checklist_code: 'PRE_PREG_04', order: 1, item_text: 'Ăn uống đa dạng, đủ chất và sử dụng muối iod', description: 'Ăn đa dạng, đủ chất và sử dụng muối hoặc bột canh có iod.', is_required: 'TRUE', support_function: 'CONTENT_LIBRARY', source_url: '' },
    { checklist_code: 'PRE_PREG_04', order: 2, item_text: 'Duy trì cân nặng và chỉ số BMI hợp lý', description: 'Đạt và duy trì cân nặng phù hợp; tài liệu đưa ra BMI 18,5–24 kg/m².', is_required: 'TRUE', support_function: 'MATERNAL_HEALTH_METRICS', source_url: '' },
    { checklist_code: 'PRE_PREG_04', order: 3, item_text: 'Tập thể dục thường xuyên, nghỉ ngơi hợp lý', description: 'Tập thể dục thường xuyên; bố trí lao động và nghỉ ngơi hợp lý.', is_required: 'FALSE', support_function: 'MATERNAL_EXERCISES', source_url: '' },
    { checklist_code: 'PRE_PREG_04', order: 4, item_text: 'Tránh rượu bia, thuốc lá và chất kích thích', description: 'Không uống rượu bia, hút thuốc, sử dụng chất gây nghiện; tránh hít khói thuốc.', is_required: 'TRUE', support_function: 'CONTENT_LIBRARY', source_url: '' },
    { checklist_code: 'PRE_PREG_04', order: 5, item_text: 'Tránh tiếp xúc hóa chất độc hại', description: 'Tránh hóa chất độc hại, thuốc mạnh và thực phẩm chức năng không rõ nguồn gốc.', is_required: 'TRUE', support_function: 'CONTENT_LIBRARY', source_url: '' },
    { checklist_code: 'PRE_PREG_04', order: 6, item_text: 'Giữ vệ sinh và tẩy giun định kỳ', description: 'Giữ vệ sinh cá nhân, vệ sinh môi trường và tẩy giun 1–2 lần mỗi năm.', is_required: 'FALSE', support_function: 'CONTENT_LIBRARY', source_url: '' },

    // Items for PRE_PREG_05
    { checklist_code: 'PRE_PREG_05', order: 1, item_text: 'Bổ sung Sắt và Axit Folic trước thai kỳ', description: 'Bổ sung sắt 30–60 mg/ngày và axit folic 400–800 mcg/ngày, ít nhất 3 tháng trước khi có thai.', is_required: 'TRUE', support_function: 'REMINDERS', source_url: '' },
    { checklist_code: 'PRE_PREG_05', order: 2, item_text: 'Tư vấn Axit Folic liều cao nếu có tiền sử dị tật', description: 'Nếu từng có thai hoặc sinh con bị dị tật ống thần kinh, trao đổi với bác sĩ về axit folic 4 mg/ngày.', is_required: 'FALSE', support_function: 'EXPERT_CONSULTATION', source_url: '' },
    { checklist_code: 'PRE_PREG_05', order: 3, item_text: 'Rà soát lịch sử tiêm chủng cá nhân', description: 'Xác định mình đã từng tiêm các vắc-xin được khuyến cáo hay chưa và hoàn thành liều còn thiếu theo tư vấn.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'PRE_PREG_05', order: 4, item_text: 'Tiêm các vắc-xin thiết yếu trước mang thai', description: 'Xem xét các vắc-xin tài liệu đề cập: uốn ván, cúm, rubella, thủy đậu, bạch hầu, ho gà, HPV và viêm gan B.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'PRE_PREG_05', order: 5, item_text: 'Tuân thủ khoảng cách sau tiêm MMR và thủy đậu', description: 'Với MMR và thủy đậu, tiêm trước khi mang thai và tuân thủ thời gian chờ theo nhà sản xuất.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },

    // Items for PRE_PREG_06
    { checklist_code: 'PRE_PREG_06', order: 1, item_text: 'Trang bị kiến thức làm mẹ và chăm sóc trẻ', description: 'Trang bị kiến thức làm mẹ và chăm sóc trẻ.', is_required: 'TRUE', support_function: 'CONTENT_LIBRARY', source_url: '' },
    { checklist_code: 'PRE_PREG_06', order: 2, item_text: 'Theo dõi chu kỳ kinh nguyệt để nhận biết ngày rụng trứng', description: 'Theo dõi chu kỳ kinh nguyệt, nhiệt độ cơ thể và chất nhầy âm đạo để nhận biết thời gian dễ thụ thai.', is_required: 'TRUE', support_function: 'JOURNEY', source_url: '' },
    { checklist_code: 'PRE_PREG_06', order: 3, item_text: 'Khuyến khích bạn đời duy trì lối sống lành mạnh', description: 'Khuyến khích người chồng tránh rượu, thuốc lá, chất gây nghiện và tránh quần áo quá chật, nóng.', is_required: 'FALSE', support_function: 'CONTENT_LIBRARY', source_url: '' },
    { checklist_code: 'PRE_PREG_06', order: 4, item_text: 'Hoàn tất sàng lọc và chuẩn bị tâm lý', description: 'Hoàn tất tư vấn, sàng lọc và quản lý nguy cơ; nếu bệnh lý phức tạp, đến bệnh viện chuyên khoa phù hợp.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },

    // Items for PREG_ONCE_01
    { checklist_code: 'PREG_ONCE_01', order: 1, item_text: 'Đi khám thai lần đầu', description: 'Khám thai định kỳ xác định vị trí và tình trạng thai ban đầu.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'PREG_ONCE_01', order: 2, item_text: 'Xét nghiệm haemoglobin phát hiện thiếu máu', description: 'Xét nghiệm haemoglobin để phát hiện thiếu máu.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'PREG_ONCE_01', order: 3, item_text: 'Xác định nhóm máu và tình trạng Rh', description: 'Xác định nhóm máu ABO và tình trạng Rh (+/-).', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'PREG_ONCE_01', order: 4, item_text: 'Sàng lọc HIV, giang mai, viêm gan B', description: 'Sàng lọc các bệnh truyền nhiễm lây truyền từ mẹ sang con.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'PREG_ONCE_01', order: 5, item_text: 'Sàng lọc dị tật bẩm sinh', description: 'Sàng lọc dị tật bẩm sinh theo chỉ định của bác sĩ.', is_required: 'FALSE', support_function: 'HEALTH_RECORDS', source_url: '' },

    // Items for PREG_ONCE_02
    { checklist_code: 'PREG_ONCE_02', order: 1, item_text: 'Thực hiện siêu âm hình thái học trước tuần 24', description: 'Thực hiện hoặc xác nhận đã thực hiện siêu âm trước tuần 24.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'PREG_ONCE_02', order: 2, item_text: 'Hoàn thành xét nghiệm/sàng lọc còn thiếu', description: 'Hoàn thành các xét nghiệm hoặc sàng lọc còn thiếu từ lần đầu.', is_required: 'FALSE', support_function: 'HEALTH_RECORDS', source_url: '' },

    // Items for PREG_ONCE_03
    { checklist_code: 'PREG_ONCE_03', order: 1, item_text: 'Xét nghiệm đường huyết thai kỳ (OGTT)', description: 'Xét nghiệm đường huyết để sàng lọc đái tháo đường thai kỳ.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'PREG_ONCE_03', order: 2, item_text: 'Kiểm tra lại kết quả nhóm máu & Rh', description: 'Kiểm tra kết quả nhóm máu và tình trạng Rh.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'PREG_ONCE_03', order: 3, item_text: 'Lên lịch tiêm Anti-D (nếu mẹ có Rh âm)', description: '(Nếu mẹ có Rh âm) Lên lịch tiêm globulin miễn dịch Anti-D vào khoảng tuần 28.', is_required: 'FALSE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'PREG_ONCE_03', order: 4, item_text: 'Theo dõi hướng dẫn y tế cho mẹ Rh âm', description: '(Nếu mẹ có Rh âm) Theo dõi và thực hiện các hướng dẫn tiếp theo của cơ sở y tế.', is_required: 'FALSE', support_function: 'HEALTH_RECORDS', source_url: '' },

    // Items for PREG_ONCE_04
    { checklist_code: 'PREG_ONCE_04', order: 1, item_text: 'Tư vấn chi tiết về kế hoạch sinh', description: 'Bắt đầu tư vấn chi tiết về kế hoạch sinh với bác sĩ.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'PREG_ONCE_04', order: 2, item_text: 'Xác định cơ sở dự kiến sinh', description: 'Lựa chọn và xác định bệnh viện/cơ sở y tế dự kiến sinh.', is_required: 'TRUE', support_function: 'JOURNEY', source_url: '' },
    { checklist_code: 'PREG_ONCE_04', order: 3, item_text: 'Lên kế hoạch xử trí tình huống khẩn cấp', description: 'Chuẩn bị danh bạ, số cấp cứu và kế hoạch xử trí khi có tình huống khẩn cấp.', is_required: 'TRUE', support_function: 'AI_TRIAGE', source_url: '' },
    { checklist_code: 'PREG_ONCE_04', order: 4, item_text: 'Tư vấn kế hoạch hóa gia đình sau sinh', description: 'Bắt đầu tìm hiểu và tư vấn kế hoạch hóa gia đình sau sinh.', is_required: 'FALSE', support_function: 'CONTENT_LIBRARY', source_url: '' },

    // Items for PREG_ONCE_05
    { checklist_code: 'PREG_ONCE_05', order: 1, item_text: 'Sàng lọc liên cầu khuẩn nhóm B (GBS)', description: 'Sàng lọc liên cầu khuẩn nhóm B — GBS, nếu được áp dụng tại cơ sở y tế.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'PREG_ONCE_05', order: 2, item_text: 'Ghi nhận kết quả GBS và phác đồ xử trí', description: 'Ghi nhận kết quả GBS và kế hoạch xử trí khi sinh nếu kết quả dương tính.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'PREG_ONCE_05', order: 3, item_text: 'Tư vấn nuôi con bằng sữa mẹ', description: 'Bắt đầu tư vấn nuôi con bằng sữa mẹ và chuẩn bị sau sinh.', is_required: 'FALSE', support_function: 'CONTENT_LIBRARY', source_url: '' },

    // Items for PREG_ONCE_06
    { checklist_code: 'PREG_ONCE_06', order: 1, item_text: 'Xác nhận cơ sở dự kiến sinh lần cuối', description: 'Xác nhận cơ sở dự kiến sinh và hồ sơ đăng ký sinh.', is_required: 'TRUE', support_function: 'JOURNEY', source_url: '' },
    { checklist_code: 'PREG_ONCE_06', order: 2, item_text: 'Xác nhận phương tiện di chuyển khi chuyển dạ', description: 'Xác nhận phương tiện di chuyển sẵn sàng 24/7.', is_required: 'TRUE', support_function: 'JOURNEY', source_url: '' },
    { checklist_code: 'PREG_ONCE_06', order: 3, item_text: 'Xác nhận người hỗ trợ khi chuyển dạ', description: 'Xác nhận người thân đồng hành và hỗ trợ khi chuyển dạ.', is_required: 'TRUE', support_function: 'JOURNEY', source_url: '' },
    { checklist_code: 'PREG_ONCE_06', order: 4, item_text: 'Tìm hiểu các dấu hiệu chuyển dạ cần đến viện', description: 'Tìm hiểu các dấu hiệu cần đến cơ sở y tế ngay (vỡ ối, ra máu, co thắt dồn dập).', is_required: 'TRUE', support_function: 'AI_TRIAGE', source_url: '' },

    // Items for PREG_ONCE_07
    { checklist_code: 'PREG_ONCE_07', order: 1, item_text: 'Rà soát lần cuối kế hoạch sinh', description: 'Rà soát đồ dùng đi sinh và kế hoạch sinh.', is_required: 'TRUE', support_function: 'JOURNEY', source_url: '' },
    { checklist_code: 'PREG_ONCE_07', order: 2, item_text: 'Rà soát phương án đi lại và hỗ trợ', description: 'Rà soát phương án đi lại và người hỗ trợ khi chuyển dạ.', is_required: 'TRUE', support_function: 'JOURNEY', source_url: '' },
    { checklist_code: 'PREG_ONCE_07', order: 3, item_text: 'Trao đổi kế hoạch theo dõi nếu chưa sinh', description: 'Trao đổi với nhân viên y tế về kế hoạch theo dõi tiếp theo nếu quá ngày dự sinh.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },

    // Items for PREG_WEEKLY_01
    { checklist_code: 'PREG_WEEKLY_01', order: 1, item_text: 'Đo huyết áp hàng tuần', description: 'Đo huyết áp và ghi nhận vào nhật ký sức khỏe.', is_required: 'TRUE', support_function: 'MATERNAL_HEALTH_METRICS', source_url: '' },
    { checklist_code: 'PREG_WEEKLY_01', order: 2, item_text: 'Đo cân nặng và cập nhật chỉ số BMI', description: 'Đo cân nặng và cập nhật BMI hàng tuần.', is_required: 'TRUE', support_function: 'MATERNAL_HEALTH_METRICS', source_url: '' },

    // Items for PREG_WEEKLY_02
    { checklist_code: 'PREG_WEEKLY_02', order: 1, item_text: 'Kiểm tra protein niệu sàng lọc tiền sản giật', description: 'Kiểm tra protein niệu để sàng lọc tiền sản giật.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },

    // Items for PREG_WEEKLY_03
    { checklist_code: 'PREG_WEEKLY_03', order: 1, item_text: 'Theo dõi và đếm cử động của thai nhi', description: 'Theo dõi cử động của thai nhi hàng tuần/hàng ngày từ tuần 30.', is_required: 'TRUE', support_function: 'MATERNAL_HEALTH_METRICS', source_url: '' },

    // Items for PREG_DAILY_01
    { checklist_code: 'PREG_DAILY_01', order: 1, item_text: 'Bổ sung Axit Folic 400mcg/ngày', description: 'Uống Axit Folic 400mcg/ngày đều đặn.', is_required: 'TRUE', support_function: 'REMINDERS', source_url: '' },

    // Items for PREG_DAILY_02
    { checklist_code: 'PREG_DAILY_02', order: 1, item_text: 'Bổ sung Axit Folic 600mcg/ngày', description: 'Uống Axit Folic 600mcg/ngày từ tuần 21 đến khi sinh.', is_required: 'TRUE', support_function: 'REMINDERS', source_url: '' },

    // Items for POST_WEEK_01
    { checklist_code: 'POST_WEEK_01', order: 1, item_text: 'Đánh giá tâm trạng & sàng lọc trầm cảm sau sinh', description: 'Đánh giá tâm trạng, sàng lọc chứng trầm cảm sau sinh.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'POST_WEEK_01', order: 2, item_text: 'Theo dõi hồi phục vết may tầng sinh môn / vết mổ', description: 'Theo dõi quá trình hồi phục quá trình lành vết thương tầng sinh môn hoặc vết mổ lấy thai.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'POST_WEEK_01', order: 3, item_text: 'Tư vấn kế hoạch hóa gia đình sau sinh', description: 'Tư vấn về kế hoạch hóa gia đình và các lựa chọn tránh thai.', is_required: 'FALSE', support_function: 'CONTENT_LIBRARY', source_url: '' },

    // Items for POST_WEEK_02
    { checklist_code: 'POST_WEEK_02', order: 1, item_text: 'Đánh giá sức khỏe thể chất của mẹ', description: 'Đánh giá sức khỏe thể chất của mẹ (dấu hiệu sinh tồn, sự co hồi tử cung).', is_required: 'TRUE', support_function: 'MATERNAL_HEALTH_METRICS', source_url: '' },
    { checklist_code: 'POST_WEEK_02', order: 2, item_text: 'Sàng lọc sức khỏe tinh thần và trầm cảm', description: 'Sàng lọc chứng trầm cảm sau sinh và sức khỏe tinh thần.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'POST_WEEK_02', order: 3, item_text: 'Kiểm tra dấu hiệu nhiễm trùng sau sinh', description: 'Kiểm tra xem mẹ có bị nhiễm trùng không (nhiệt độ, dấu hiệu viêm vú, vết mổ lấy thai).', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'POST_WEEK_02', order: 4, item_text: 'Tư vấn dinh dưỡng, vệ sinh & cho con bú', description: 'Tư vấn về dinh dưỡng sau sinh, vệ sinh và hỗ trợ cho con bú.', is_required: 'FALSE', support_function: 'CONTENT_LIBRARY', source_url: '' },

    // Items for POST_WEEK_06
    { checklist_code: 'POST_WEEK_06', order: 1, item_text: 'Khám sức khỏe toàn diện cho mẹ mốc 6 tuần', description: 'Khám sức khỏe toàn diện cho mẹ tại cơ sở y tế.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'POST_WEEK_06', order: 2, item_text: 'Đánh giá sức khỏe tâm thần mốc 6 tuần', description: 'Đánh giá sức khỏe tâm thần của mẹ.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'POST_WEEK_06', order: 3, item_text: 'Tư vấn biện pháp tránh thai phù hợp', description: 'Tư vấn về kế hoạch hóa gia đình và các lựa chọn tránh thai.', is_required: 'FALSE', support_function: 'CONTENT_LIBRARY', source_url: '' },
    { checklist_code: 'POST_WEEK_06', order: 4, item_text: 'Tìm hiểu chăm sóc sức khỏe dài hạn', description: 'Tìm hiểu thêm về chăm sóc sức khỏe dài hạn và vận động phục hồi.', is_required: 'FALSE', support_function: 'CONTENT_LIBRARY', source_url: '' },

    // Items for BABY_0_28D
    { checklist_code: 'BABY_0_28D', order: 1, item_text: 'Khám và theo dõi sơ sinh', description: 'Xác nhận trẻ được đánh giá hô hấp, thân nhiệt, bú/ngậm bắt vú, da/vàng da, rốn, cân nặng và khám toàn thân.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'BABY_0_28D', order: 2, item_text: 'Bú mẹ và giữ ấm', description: 'Da kề da, giữ ấm và cho bú theo nhu cầu. Duy trì bú mẹ hoàn toàn; không tự thêm nước hoặc thức ăn.', is_required: 'TRUE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_0_28D', order: 3, item_text: 'Tiêm chủng sơ sinh (Viêm gan B, BCG)', description: 'Lưu mũi viêm gan B sơ sinh theo chỉ định và kiểm tra kế hoạch BCG để hoàn thành trong tháng đầu.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'BABY_0_28D', order: 4, item_text: 'Theo dõi tăng trưởng, vàng da và rốn', description: 'Theo dõi xu hướng cân nặng/chiều dài từ các lần đo đúng kỹ thuật; quan sát vàng da, rốn, mức tỉnh táo, nhịp thở và thân nhiệt.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'BABY_0_28D', order: 5, item_text: 'Tương tác sớm cùng bé', description: 'Mỗi ngày ôm, giao tiếp mắt, nói chuyện, hát và đáp lại tín hiệu của trẻ khi trẻ tỉnh táo.', is_required: 'FALSE', support_function: 'BABY_CARE', source_url: '' },

    // Items for BABY_1_2M
    { checklist_code: 'BABY_1_2M', order: 1, item_text: 'Khám mốc 6 tuần', description: 'Tại lần chăm sóc tuần 6, yêu cầu đánh giá bú mẹ, tăng trưởng, phát triển, tiêm chủng và các quan ngại của gia đình.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'BABY_1_2M', order: 2, item_text: 'Duy trì bú mẹ hoàn toàn', description: 'Tiếp tục bú mẹ hoàn toàn theo nhu cầu ngày và đêm, chưa thêm nước hoặc thức ăn.', is_required: 'TRUE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_1_2M', order: 3, item_text: 'Chuẩn bị mốc 2 tháng', description: 'Xác định ngày trẻ đủ 2 tháng theo ngày sinh thực tế; chuẩn bị sổ tiêm/giấy hẹn và đặt lịch tiêm chủng.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'BABY_1_2M', order: 4, item_text: 'Giao tiếp và phát triển', description: 'Nói chuyện, mỉm cười, hát, chơi mặt đối mặt và đáp lại tín hiệu của trẻ.', is_required: 'FALSE', support_function: 'BABY_CARE', source_url: '' },

    // Items for BABY_2_3M
    { checklist_code: 'BABY_2_3M', order: 1, item_text: 'Khám sức khỏe 2–3 tháng', description: 'Thực hiện đánh giá dấu hiệu sinh tồn, tăng trưởng–dinh dưỡng, phát triển, tình trạng tiêm chủng và khám lâm sàng trong cửa sổ 2–3 tháng.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'BABY_2_3M', order: 2, item_text: 'Tiêm chủng liều cơ bản mốc 2 tháng', description: 'Khi đủ 2 tháng, thực hiện vắc xin phối hợp (viêm gan B, bạch hầu-ho gà-uốn ván, Hib), bại liệt uống liều 1 và Rota liều 1.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'BABY_2_3M', order: 3, item_text: 'Bú mẹ hoàn toàn', description: 'Tiếp tục bú mẹ hoàn toàn theo nhu cầu ngày và đêm, không cho ăn bổ sung trước 6 tháng.', is_required: 'TRUE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_2_3M', order: 4, item_text: 'Theo dõi tăng trưởng và tương tác', description: 'Ghi cân nặng/chiều dài tại các lần khám và xem biểu đồ tăng trưởng. Tăng thời gian trò chuyện, mỉm cười, hát.', is_required: 'FALSE', support_function: 'BABY_CARE', source_url: '' },

    // Items for BABY_4_6M
    { checklist_code: 'BABY_4_6M', order: 1, item_text: 'Khám sức khỏe 4–6 tháng', description: 'Đánh giá tăng trưởng, dinh dưỡng, phát triển, tiêm chủng và khám lâm sàng trong cửa sổ 4–6 tháng.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'BABY_4_6M', order: 2, item_text: 'Hoàn thiện tiêm chủng giai đoạn đầu', description: 'Đối chiếu các liều vắc xin cơ bản còn thiếu/trễ (vắc xin phối hợp liều 2, 3, bại liệt uống, IPV liều 1 khi đủ 5 tháng).', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'BABY_4_6M', order: 3, item_text: 'Bú mẹ hoàn toàn đến đủ 6 tháng', description: 'Tiếp tục bú mẹ hoàn toàn theo nhu cầu đến đủ khoảng 6 tháng/180 ngày.', is_required: 'TRUE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_4_6M', order: 4, item_text: 'Chuẩn bị ăn bổ sung (ăn dặm)', description: 'Trước mốc 6 tháng, chuẩn bị dụng cụ sạch, thức ăn mềm/đặc phù hợp và nguyên tắc cho ăn đáp ứng.', is_required: 'FALSE', support_function: 'CONTENT_LIBRARY', source_url: '' },
    { checklist_code: 'BABY_4_6M', order: 5, item_text: 'Bắt đầu ăn bổ sung khi đủ 6 tháng', description: 'Bắt đầu với lượng nhỏ, thức ăn nghiền hoặc đặc mềm rồi tăng dần. Giai đoạn 6–8 tháng duy trì 2–3 bữa/ngày.', is_required: 'TRUE', support_function: 'BABY_CARE', source_url: '' },

    // Items for BABY_7_9M
    { checklist_code: 'BABY_7_9M', order: 1, item_text: 'Khám sức khỏe 7–9 tháng', description: 'Thực hiện đánh giá tăng trưởng–dinh dưỡng, phát triển tinh thần–vận động, tiêm chủng và khám lâm sàng.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'BABY_7_9M', order: 2, item_text: 'Ăn bổ sung 6–8 tháng', description: 'Duy trì 2–3 bữa ăn bổ sung mỗi ngày và bú mẹ theo nhu cầu. Tăng lượng và kết cấu từ từ, đa dạng nhóm thực phẩm.', is_required: 'TRUE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_7_9M', order: 3, item_text: 'Tăng kết cấu và tự ăn có giám sát', description: 'Chuyển dần từ nghiền mịn sang đặc mềm/nghiền thô; nếu trẻ sẵn sàng có thể thử thức ăn cầm tay mềm dưới sự giám sát.', is_required: 'FALSE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_7_9M', order: 4, item_text: 'Tiêm chủng mốc 9 tháng (Sởi, IPV2)', description: 'Rà soát ngày trẻ đủ 9 tháng; thực hiện vắc xin có thành phần sởi và IPV liều 2 tại cơ sở tiêm chủng.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'BABY_7_9M', order: 5, item_text: 'Chuyển tần suất ăn sau 9 tháng', description: 'Khi trẻ đủ khoảng 9 tháng, chuyển sang 3–4 bữa/ngày; có thể thêm 1–2 bữa phụ theo nhu cầu.', is_required: 'TRUE', support_function: 'BABY_CARE', source_url: '' },

    // Items for BABY_10_12M
    { checklist_code: 'BABY_10_12M', order: 1, item_text: 'Khám sức khỏe 10–12 tháng', description: 'Đánh giá dấu hiệu sinh tồn, tăng trưởng–dinh dưỡng, phát triển, tiêm chủng và khám lâm sàng.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'BABY_10_12M', order: 2, item_text: 'Dinh dưỡng và bú mẹ mốc 10–12 tháng', description: 'Duy trì 3–4 bữa/ngày, thêm 1–2 bữa phụ khi cần và tiếp tục bú mẹ. Tăng đa dạng thực phẩm giàu dinh dưỡng.', is_required: 'TRUE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_10_12M', order: 3, item_text: 'Chuyển dần sang thức ăn gia đình', description: 'Tăng dần tới thức ăn gia đình lành mạnh được cắt hoặc nghiền phù hợp khả năng nhai nuốt.', is_required: 'FALSE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_10_12M', order: 4, item_text: 'Rà soát lịch sử tiêm chủng', description: 'Mở sổ tiêm từ sơ sinh đến 11 tháng, đánh dấu mũi đã tiêm, mũi trễ và mũi cần bù.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'BABY_10_12M', order: 5, item_text: 'Chuẩn bị kiểm tra mốc 12 tháng', description: 'Đặt lịch kiểm tra khi trẻ đủ 12 tháng để đánh giá tăng trưởng, phát triển và rà soát lịch Viêm não Nhật Bản B.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },

    // Items for BABY_12M
    { checklist_code: 'BABY_12M', order: 1, item_text: 'Khám mốc 12 tháng (1 tuổi)', description: 'Đo cân nặng/chiều dài, đánh giá phát triển, dinh dưỡng, răng miệng và các quan ngại của gia đình.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'BABY_12M', order: 2, item_text: 'Tiêm Viêm não Nhật Bản B liều 1', description: 'Thực hiện liều 1 khi trẻ đủ 12 tháng; ghi ngày và tên vắc xin, sau đó đặt lời nhắc liều 2 sau 1–2 tuần.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },
    { checklist_code: 'BABY_12M', order: 3, item_text: 'Dinh dưỡng sau 1 tuổi', description: 'Duy trì 3–4 bữa chính và 1–2 bữa phụ giàu dinh dưỡng, nước sạch và bú mẹ theo nhu cầu đến 2 tuổi.', is_required: 'TRUE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_12M', order: 4, item_text: 'Vận động, giấc ngủ và tương tác', description: 'Tăng cơ hội vận động hằng ngày (~180 phút); duy trì 11–14 giờ ngủ/ngày; tránh màn hình thụ động ở trẻ 1 tuổi.', is_required: 'FALSE', support_function: 'BABY_CARE', source_url: '' },

    // Items for BABY_13_18M
    { checklist_code: 'BABY_13_18M', order: 1, item_text: 'Khám sức khỏe 13–18 tháng', description: 'Thực hiện đánh giá dấu hiệu sinh tồn, tăng trưởng–dinh dưỡng, phát triển tinh thần–vận động và tiêm chủng.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'BABY_13_18M', order: 2, item_text: 'Ăn uống và tự ăn độc lập', description: 'Duy trì 3–4 bữa chính và 1–2 bữa phụ; cho trẻ tự xúc/cầm thức ăn mềm dưới sự giám sát.', is_required: 'TRUE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_13_18M', order: 3, item_text: 'Vận động và giấc ngủ đều đặn', description: 'Tạo ít nhất khoảng 180 phút vận động đa dạng trong không gian an toàn; duy trì khoảng 11–14 giờ ngủ/ngày.', is_required: 'FALSE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_13_18M', order: 4, item_text: 'Ngôn ngữ, chơi và phát triển giao tiếp', description: 'Mỗi ngày đọc sách, nói tên đồ vật, hát, chơi giả vờ và khuyến khích trẻ dùng từ/cử chỉ để giao tiếp.', is_required: 'FALSE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_13_18M', order: 5, item_text: 'Chăm sóc răng miệng với kem có fluor', description: 'Người lớn chải răng cho trẻ 2 lần/ngày bằng lượng kem đánh răng có fluor cỡ hạt gạo; không để trẻ tự nuốt kem.', is_required: 'TRUE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_13_18M', order: 6, item_text: 'Tiêm chủng mốc 18 tháng (Sởi-Rubella, DPT nhắc lại)', description: 'Khi đủ 18 tháng, thực hiện mũi sởi–rubella và bạch hầu–ho gà–uốn ván nhắc lại tại cơ sở tiêm chủng.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },

    // Items for BABY_19_24M
    { checklist_code: 'BABY_19_24M', order: 1, item_text: 'Khám sức khỏe 19–<24 tháng', description: 'Đánh giá tăng trưởng–dinh dưỡng, phát triển tinh thần–vận động, tiêm chủng và khám lâm sàng.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'BABY_19_24M', order: 2, item_text: 'Dinh dưỡng và bú mẹ đến 2 tuổi', description: 'Duy trì 3–4 bữa chính, 1–2 bữa phụ đa dạng; ưu tiên thực phẩm giàu dinh dưỡng và tiếp tục bú mẹ nếu phù hợp.', is_required: 'TRUE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_19_24M', order: 3, item_text: 'Vận động, chơi và mở rộng giao tiếp', description: 'Duy trì khoảng 180 phút vận động mỗi ngày; đọc truyện, hát, chơi giả vờ và mở rộng vốn từ của trẻ.', is_required: 'FALSE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_19_24M', order: 4, item_text: 'Chăm sóc răng miệng hàng ngày', description: 'Tiếp tục chải răng 2 lần/ngày bằng lượng kem fluor cỡ hạt gạo; hạn chế đồ ngọt giữa các bữa.', is_required: 'TRUE', support_function: 'BABY_CARE', source_url: '' },
    { checklist_code: 'BABY_19_24M', order: 5, item_text: 'Chuẩn bị và thực hiện kiểm tra 24 tháng', description: 'Đặt lịch mốc 24 tháng để đánh giá tăng trưởng, phát triển, dinh dưỡng, răng miệng, nghe/nhìn và rà soát tiêm chủng.', is_required: 'TRUE', support_function: 'APPOINTMENTS', source_url: '' },
    { checklist_code: 'BABY_19_24M', order: 6, item_text: 'Tiêm Viêm não Nhật Bản B liều 3', description: 'Kiểm tra ngày liều 2; tiêm liều 3 sau đúng khoảng 1 năm sau liều 2 theo lịch cơ sở tiêm chủng.', is_required: 'TRUE', support_function: 'HEALTH_RECORDS', source_url: '' },

    // Items for BABY_SAFETY_0_24M
    { checklist_code: 'BABY_SAFETY_0_24M', order: 1, item_text: 'Dấu hiệu cần đưa trẻ đi khám/cấp cứu ngay', description: 'Đưa trẻ đi cấp cứu/khám ngay khi có khó thở, tím tái, co giật, li bì/khó đánh thức, bỏ bú, sốt cao kéo dài hoặc mất nước.', is_required: 'TRUE', support_function: 'AI_TRIAGE', source_url: '' },
  ];

  itemRows.forEach((r) => {
    const row = sheet2.addRow(r);
    row.height = 24;
    row.eachCell((cell, colNumber) => {
      cell.border = borderThin;
      cell.font = { name: 'Calibri', size: 10 };
      if ([1, 2, 5, 6].includes(colNumber)) {
        cell.alignment = { vertical: 'middle', horizontal: 'center' };
      } else {
        cell.alignment = { vertical: 'middle', horizontal: 'left' };
      }
    });
  });

  sheet2.autoFilter = `A1:G${itemRows.length + 1}`;

  // Data validations for Checklist_Items
  for (let rowIdx = 2; rowIdx <= 200; rowIdx++) {
    sheet2.getCell(`E${rowIdx}`).dataValidation = {
      type: 'list',
      allowBlank: true,
      formulae: ['"TRUE,FALSE"'],
    };
    sheet2.getCell(`F${rowIdx}`).dataValidation = {
      type: 'list',
      allowBlank: true,
      formulae: ['"HEALTH_RECORDS,MATERNAL_HEALTH_METRICS,MATERNAL_EXERCISES,APPOINTMENTS,REMINDERS,JOURNEY,BABY_CARE,EXPERT_CONSULTATION,CONTENT_LIBRARY,AI_TRIAGE"'],
    };
  }

  // ==========================================
  // SHEET 3: Huong_dan
  // ==========================================
  const sheet3 = workbook.addWorksheet('Huong_dan');
  sheet3.columns = [
    { header: 'STT / Mục', key: 'col1', width: 28 },
    { header: 'Nội dung hướng dẫn chi tiết', key: 'col2', width: 110 },
  ];

  const headerRow3 = sheet3.getRow(1);
  headerRow3.height = 28;
  headerRow3.eachCell((cell) => {
    cell.fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FF334155' }, // Slate gray
    };
    cell.font = headerFont;
    cell.alignment = { vertical: 'middle', horizontal: 'center' };
    cell.border = borderThin;
  });

  const instructions = [
    ['1. Cấu trúc file Excel', 'File gồm 3 sheet: "Checklists" (danh sách checklist cha), "Checklist_Items" (các mục con thuộc checklist) và "Huong_dan" (tài liệu quy chuẩn).'],
    ['', 'Tuyệt đối KHÔNG thay đổi tên sheet, thứ tự hoặc tên các cột tiêu đề ở dòng 1.'],
    ['2. Liên kết cha - con', 'Mỗi mục trong sheet "Checklist_Items" liên kết với checklist tương ứng trong sheet "Checklists" qua cột "checklist_code" (không phân biệt hoa/thường).'],
    ['3. Cột stage', 'Các giá trị hợp lệ: PRE_PREGNANCY (Trước thai kỳ), PREGNANCY (Trong thai kỳ), POSTPARTUM (Sau sinh), BABY_CARE (Chăm sóc bé).'],
    ['4. Cột template_type', 'Các giá trị hợp lệ: MANDATORY (Bắt buộc), OPTIONAL (Tùy chọn).'],
    ['5. Cột repeat_mode', 'Các giá trị hợp lệ: NONE (Không lặp), WEEKLY (Hàng tuần), DAILY (Hàng ngày). Lưu ý: PRE_PREGNANCY không hỗ trợ WEEKLY.'],
    ['6. Cột end_at_stage_exit', 'TRUE: checklist kết thúc khi kết thúc giai đoạn. FALSE: checklist kết thúc tại window_end.'],
    ['7. window_start / window_end', 'Tuần hiển thị tính từ 1 đến 52. Khi end_at_stage_exit = TRUE, có thể để trống window_end.'],
    ['', 'Đối với PRE_PREGNANCY, để trống cả window_start và window_end.'],
    ['8. Cột display_order', 'Số nguyên không âm (0, 1, 2...). Đối với PRE_PREGNANCY loại MANDATORY, display_order phải từ 1 trở lên.'],
    ['9. Cột is_required (ở Items)', 'TRUE: Mục con bắt buộc phải hoàn thành. FALSE: Mục con tùy chọn.'],
    ['10. Cột support_function', 'Chức năng tích hợp trong ứng dụng CareBridge (để trống nếu không liên kết):'],
    ['', '• HEALTH_RECORDS: Hồ sơ sức khỏe (tiêm chủng, xét nghiệm, hồ sơ bệnh án)'],
    ['', '• MATERNAL_HEALTH_METRICS: Chỉ số sức khỏe mẹ (huyết áp, cân nặng, cử động thai)'],
    ['', '• MATERNAL_EXERCISES: Bài tập cho mẹ bầu / phục hồi sau sinh'],
    ['', '• APPOINTMENTS: Đặt lịch khám bệnh / tiêm chủng'],
    ['', '• REMINDERS: Nhắc nhở uống thuốc / bổ sung vi chất hàng ngày'],
    ['', '• JOURNEY: Hành trình làm mẹ (chu kỳ, kế hoạch sinh, nhật ký bé)'],
    ['', '• BABY_CARE: Nhật ký chăm sóc bé (cữ bú, tã, giấc ngủ, vệ sinh)'],
    ['', '• EXPERT_CONSULTATION: Tư vấn chuyên gia'],
    ['', '• CONTENT_LIBRARY: Thư viện kiến thức'],
    ['', '• AI_TRIAGE: Trợ lý AI sàng lọc / dấu hiệu cấp cứu'],
    ['11. Cột source_url', 'Đường dẫn tham khảo uy tín (bắt đầu bằng http:// hoặc https://, tối đa 2048 ký tự). Để trống nếu chưa có URL.'],
    ['12. Quy tắc thẩm định (Validation)', '• Một mục con bị lỗi sẽ khiến toàn bộ checklist cha đó bị báo lỗi và không được import.'],
    ['', '• Mã checklist không được trùng lặp trong sheet Checklists.'],
    ['', '• Thứ tự (order) của các mục con trong cùng một checklist không được trùng nhau.'],
    ['', '• Mỗi lần import hỗ trợ tối đa 100 checklist hợp lệ, dung lượng file tối đa 5MB.'],
  ];

  instructions.forEach(([col1, col2]) => {
    const row = sheet3.addRow({ col1, col2 });
    row.height = 22;
    row.eachCell((cell, colNumber) => {
      cell.border = borderThin;
      cell.font = { name: 'Calibri', size: 10 };
      cell.alignment = { vertical: 'middle', horizontal: 'left', wrapText: true };
    });
  });

  const buffer = await workbook.xlsx.writeBuffer();

  const rootPath = path.resolve(__dirname, '../../../Form_Mau_Import_Checklist.xlsx');
  fs.writeFileSync(rootPath, Buffer.from(buffer));
  console.log('Saved to root:', rootPath);

  const publicPath = path.resolve(__dirname, '../public/Form_Mau_Import_Checklist.xlsx');
  fs.writeFileSync(publicPath, Buffer.from(buffer));
  console.log('Saved to public:', publicPath);

  const distPath = path.resolve(__dirname, '../dist/Form_Mau_Import_Checklist.xlsx');
  if (fs.existsSync(path.dirname(distPath))) {
    fs.writeFileSync(distPath, Buffer.from(buffer));
    console.log('Saved to dist:', distPath);
  }

  console.log(`Generated ${rootRows.length} Checklists and ${itemRows.length} Checklist Items successfully!`);
}

buildFilledTemplate().catch(console.error);
