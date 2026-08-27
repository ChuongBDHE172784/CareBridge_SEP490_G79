const ExcelJS = require('exceljs');
const fs = require('fs');
const path = require('path');

async function createTemplate() {
  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'CareBridge System';
  workbook.created = new Date();
  workbook.modified = new Date();

  // Color palette
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
    { header: 'name', key: 'name', width: 36 },
    { header: 'description', key: 'description', width: 48 },
    { header: 'stage', key: 'stage', width: 22 },
    { header: 'template_type', key: 'template_type', width: 18 },
    { header: 'window_start', key: 'window_start', width: 15 },
    { header: 'window_end', key: 'window_end', width: 15 },
    { header: 'end_at_stage_exit', key: 'end_at_stage_exit', width: 20 },
    { header: 'display_order', key: 'display_order', width: 16 },
    { header: 'repeat_mode', key: 'repeat_mode', width: 18 },
  ];

  // Format header row
  const headerRow1 = sheet1.getRow(1);
  headerRow1.height = 28;
  headerRow1.eachCell((cell) => {
    cell.fill = primaryHeaderFill;
    cell.font = headerFont;
    cell.alignment = { vertical: 'middle', horizontal: 'center' };
    cell.border = borderThin;
  });

  const rootRows = [
    {
      checklist_code: 'PREG-PLAN-01',
      name: 'Khám thai định kỳ 3 tháng đầu',
      description: 'Các việc quan trọng cần làm trong 3 tháng đầu thai kỳ',
      stage: 'PREGNANCY',
      template_type: 'MANDATORY',
      window_start: 1,
      window_end: 13,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'WEEKLY',
    },
    {
      checklist_code: 'PRE-PREG-01',
      name: 'Chuẩn bị trước mang thai',
      description: 'Kế hoạch chăm sóc sức khỏe và dinh dưỡng tiền thai kỳ',
      stage: 'PRE_PREGNANCY',
      template_type: 'MANDATORY',
      window_start: '',
      window_end: '',
      end_at_stage_exit: 'FALSE',
      display_order: 1,
      repeat_mode: 'NONE',
    },
    {
      checklist_code: 'POST-RECOV-01',
      name: 'Phục hồi và chăm sóc sau sinh',
      description: 'Hướng dẫn theo dõi sức khỏe mẹ trong 6 tuần đầu sau sinh',
      stage: 'POSTPARTUM',
      template_type: 'MANDATORY',
      window_start: 1,
      window_end: 6,
      end_at_stage_exit: 'FALSE',
      display_order: 0,
      repeat_mode: 'WEEKLY',
    },
    {
      checklist_code: 'BABY-CARE-01',
      name: 'Chăm sóc bé sơ sinh 0-3 tháng',
      description: 'Theo dõi dinh dưỡng, giấc ngủ và phát triển của bé',
      stage: 'BABY_CARE',
      template_type: 'OPTIONAL',
      window_start: 1,
      window_end: 12,
      end_at_stage_exit: 'TRUE',
      display_order: 0,
      repeat_mode: 'DAILY',
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

  sheet1.autoFilter = 'A1:J5';

  // Data validations for Checklists
  for (let rowIdx = 2; rowIdx <= 200; rowIdx++) {
    sheet1.getCell(`D${rowIdx}`).dataValidation = {
      type: 'list',
      allowBlank: true,
      formulae: ['"PRE_PREGNANCY,PREGNANCY,POSTPARTUM,BABY_CARE"'],
      showErrorMessage: true,
      errorTitle: 'Giá trị không hợp lệ',
      error: 'Vui lòng chọn từ danh sách: PRE_PREGNANCY, PREGNANCY, POSTPARTUM, BABY_CARE',
    };
    sheet1.getCell(`E${rowIdx}`).dataValidation = {
      type: 'list',
      allowBlank: true,
      formulae: ['"MANDATORY,OPTIONAL"'],
      showErrorMessage: true,
      errorTitle: 'Giá trị không hợp lệ',
      error: 'Vui lòng chọn MANDATORY hoặc OPTIONAL',
    };
    sheet1.getCell(`H${rowIdx}`).dataValidation = {
      type: 'list',
      allowBlank: true,
      formulae: ['"TRUE,FALSE"'],
      showErrorMessage: true,
      errorTitle: 'Giá trị không hợp lệ',
      error: 'Vui lòng chọn TRUE hoặc FALSE',
    };
    sheet1.getCell(`J${rowIdx}`).dataValidation = {
      type: 'list',
      allowBlank: true,
      formulae: ['"NONE,WEEKLY,DAILY"'],
      showErrorMessage: true,
      errorTitle: 'Giá trị không hợp lệ',
      error: 'Vui lòng chọn NONE, WEEKLY hoặc DAILY',
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
    { header: 'description', key: 'description', width: 48 },
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
    {
      checklist_code: 'PREG-PLAN-01',
      order: 1,
      item_text: 'Đặt lịch khám thai lần đầu (tuần 6-8)',
      description: 'Siêu âm xác định tim thai và vị trí thai',
      is_required: 'TRUE',
      support_function: 'APPOINTMENTS',
      source_url: 'https://moh.gov.vn',
    },
    {
      checklist_code: 'PREG-PLAN-01',
      order: 2,
      item_text: 'Ghi lại chỉ số huyết áp và cân nặng',
      description: 'Cập nhật định kỳ vào hồ sơ theo dõi sức khỏe',
      is_required: 'TRUE',
      support_function: 'MATERNAL_HEALTH_METRICS',
      source_url: '',
    },
    {
      checklist_code: 'PREG-PLAN-01',
      order: 3,
      item_text: 'Tập bài tập thở và giãn cơ cho mẹ bầu',
      description: 'Duy trì 15-20 phút mỗi ngày',
      is_required: 'FALSE',
      support_function: 'MATERNAL_EXERCISES',
      source_url: '',
    },
    {
      checklist_code: 'PRE-PREG-01',
      order: 1,
      item_text: 'Tiêm phòng trước mang thai (Rubella, Cúm)',
      description: 'Tiêm ít nhất 1-3 tháng trước khi có thai',
      is_required: 'TRUE',
      support_function: 'HEALTH_RECORDS',
      source_url: 'https://moh.gov.vn',
    },
    {
      checklist_code: 'PRE-PREG-01',
      order: 2,
      item_text: 'Bổ sung Acid Folic hàng ngày (400mcg)',
      description: 'Uống đều đặn theo khung giờ cố định',
      is_required: 'TRUE',
      support_function: 'REMINDERS',
      source_url: '',
    },
    {
      checklist_code: 'POST-RECOV-01',
      order: 1,
      item_text: 'Theo dõi phục hồi vết mổ / vết may',
      description: 'Ghi chú các dấu hiệu bất thường nếu có',
      is_required: 'TRUE',
      support_function: 'HEALTH_RECORDS',
      source_url: '',
    },
    {
      checklist_code: 'POST-RECOV-01',
      order: 2,
      item_text: 'Luyện tập cơ sàn chậu nhẹ nhàng (Kegel)',
      description: 'Giúp hỗ trợ phục hồi cơ sàn chậu sau sinh',
      is_required: 'FALSE',
      support_function: 'MATERNAL_EXERCISES',
      source_url: '',
    },
    {
      checklist_code: 'POST-RECOV-01',
      order: 3,
      item_text: 'Đặt lịch khám kiểm tra 6 tuần sau sinh',
      description: 'Đánh giá tổng quát mức độ hồi phục của mẹ',
      is_required: 'TRUE',
      support_function: 'APPOINTMENTS',
      source_url: 'https://moh.gov.vn',
    },
    {
      checklist_code: 'BABY-CARE-01',
      order: 1,
      item_text: 'Theo dõi cữ bú và tã ướt của bé',
      description: 'Ghi lại số lần bú mẹ/sữa công thức mỗi ngày',
      is_required: 'TRUE',
      support_function: 'BABY_CARE',
      source_url: '',
    },
    {
      checklist_code: 'BABY-CARE-01',
      order: 2,
      item_text: 'Vệ sinh rốn và chăm sóc da bé',
      description: 'Giữ rốn khô thoáng, kiểm tra dấu hiệu nhiễm trùng',
      is_required: 'TRUE',
      support_function: 'BABY_CARE',
      source_url: '',
    },
    {
      checklist_code: 'BABY-CARE-01',
      order: 3,
      item_text: 'Đo thân nhiệt và theo dõi sức khỏe bé',
      description: 'Đo nhiệt độ khi bé có biểu hiện quấy khóc',
      is_required: 'FALSE',
      support_function: 'HEALTH_RECORDS',
      source_url: '',
    },
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

  sheet2.autoFilter = 'A1:G12';

  // Data validations for Checklist_Items
  for (let rowIdx = 2; rowIdx <= 500; rowIdx++) {
    sheet2.getCell(`E${rowIdx}`).dataValidation = {
      type: 'list',
      allowBlank: true,
      formulae: ['"TRUE,FALSE"'],
      showErrorMessage: true,
      errorTitle: 'Giá trị không hợp lệ',
      error: 'Vui lòng chọn TRUE hoặc FALSE',
    };
    sheet2.getCell(`F${rowIdx}`).dataValidation = {
      type: 'list',
      allowBlank: true,
      formulae: ['"HEALTH_RECORDS,MATERNAL_HEALTH_METRICS,MATERNAL_EXERCISES,APPOINTMENTS,REMINDERS,JOURNEY,BABY_CARE,EXPERT_CONSULTATION,CONTENT_LIBRARY,AI_TRIAGE"'],
      showErrorMessage: true,
      errorTitle: 'Giá trị không hợp lệ',
      error: 'Vui lòng chọn chức năng hợp lệ từ danh sách',
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
    ['10. Cột support_function', 'Chức năng tích hợp trong ứng dụng CareBridge (có thể để trống nếu không liên kết):'],
    ['', '• HEALTH_RECORDS: Hồ sơ sức khỏe'],
    ['', '• MATERNAL_HEALTH_METRICS: Chỉ số sức khỏe mẹ'],
    ['', '• MATERNAL_EXERCISES: Bài tập cho mẹ bầu / sau sinh'],
    ['', '• APPOINTMENTS: Đặt lịch khám'],
    ['', '• REMINDERS: Nhắc nhở'],
    ['', '• JOURNEY: Hành trình làm mẹ'],
    ['', '• BABY_CARE: Nhật ký chăm sóc bé'],
    ['', '• EXPERT_CONSULTATION: Tư vấn chuyên gia'],
    ['', '• CONTENT_LIBRARY: Thư viện kiến thức'],
    ['', '• AI_TRIAGE: Trợ lý AI sàng lọc'],
    ['11. Cột source_url', 'Đường dẫn tham khảo uy tín (bắt đầu bằng http:// hoặc https://, tối đa 2048 ký tự). Có thể để trống.'],
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
      cell.alignment = { vertical: 'middle', horizontal: colNumber === 1 ? 'left' : 'left', wrapText: true };
    });
  });

  const buffer = await workbook.xlsx.writeBuffer();

  const rootPath = path.resolve(__dirname, '../../../Form_Mau_Import_Checklist.xlsx');
  fs.writeFileSync(rootPath, Buffer.from(buffer));
  console.log('Saved root template:', rootPath);

  const publicPath = path.resolve(__dirname, '../public/Form_Mau_Import_Checklist.xlsx');
  fs.writeFileSync(publicPath, Buffer.from(buffer));
  console.log('Saved public template:', publicPath);

  const distPath = path.resolve(__dirname, '../dist/Form_Mau_Import_Checklist.xlsx');
  if (fs.existsSync(path.dirname(distPath))) {
    fs.writeFileSync(distPath, Buffer.from(buffer));
    console.log('Saved dist template:', distPath);
  }
}

createTemplate().then(() => console.log('Successfully created ExcelJS template!')).catch(console.error);
