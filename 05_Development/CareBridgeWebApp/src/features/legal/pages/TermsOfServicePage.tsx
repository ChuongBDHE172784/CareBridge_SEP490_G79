import LegalPageLayout, { Bullets, LawRef, P } from '../components/LegalPageLayout';
import type { LegalSection } from '../components/LegalPageLayout';

/**
 * Điều khoản sử dụng dịch vụ.
 *
 * Trọng tâm là tư cách chuyên gia y tế, vì đây là tài liệu người dùng phải chấp
 * thuận ở màn đăng ký chuyên gia. Nội dung bám vào hai ràng buộc pháp lý thực
 * tế: điều kiện hành nghề theo Luật Khám bệnh, chữa bệnh, và giá trị pháp lý của
 * việc chấp thuận điện tử theo Luật Giao dịch điện tử.
 */

const CONTACT_EMAIL = 'support@carebridgevn.site';

const sections: LegalSection[] = [
  {
    id: 'chap-nhan',
    title: 'Chấp nhận điều khoản',
    body: (
      <>
        <P>
          Khi đánh dấu vào ô chấp thuận và hoàn tất đăng ký, bạn xác nhận đã đọc, hiểu và đồng ý bị
          ràng buộc bởi Điều khoản sử dụng này cùng Chính sách bảo mật dữ liệu cá nhân của CareBridge.
        </P>
        <P>
          Việc chấp thuận bằng thao tác điện tử có giá trị pháp lý tương đương văn bản theo{' '}
          <LawRef>Luật Giao dịch điện tử số 20/2023/QH15</LawRef>. Hệ thống ghi nhận thời điểm bạn
          chấp thuận và phiên bản tài liệu tại thời điểm đó.
        </P>
        <P>Nếu bạn không đồng ý, vui lòng không sử dụng dịch vụ.</P>
      </>
    ),
  },
  {
    id: 'dinh-nghia',
    title: 'Định nghĩa',
    body: (
      <Bullets
        items={[
          <>
            <LawRef>CareBridge</LawRef> — nền tảng kết nối chăm sóc sức khỏe bà mẹ và trẻ nhỏ, gồm
            ứng dụng web và ứng dụng di động.
          </>,
          <>
            <LawRef>Chuyên gia</LawRef> — người hành nghề khám bệnh, chữa bệnh đã được CareBridge
            xác minh và cho phép cung cấp dịch vụ tư vấn trên nền tảng.
          </>,
          <>
            <LawRef>Người dùng</LawRef> — người mẹ, thành viên gia đình sử dụng dịch vụ.
          </>,
          <>
            <LawRef>Nội dung</LawRef> — mọi thông tin, hình ảnh, tài liệu do người dùng hoặc chuyên
            gia đăng tải, gửi đi trên nền tảng.
          </>,
        ]}
      />
    ),
  },
  {
    id: 'dieu-kien-chuyen-gia',
    title: 'Điều kiện trở thành chuyên gia',
    body: (
      <>
        <P>Để được phê duyệt tư cách chuyên gia, bạn phải đáp ứng đồng thời:</P>
        <Bullets
          items={[
            <>
              Có <LawRef>giấy phép hành nghề khám bệnh, chữa bệnh</LawRef> còn hiệu lực do cơ quan có
              thẩm quyền cấp theo Luật Khám bệnh, chữa bệnh số 15/2023/QH15.
            </>,
            'Hành nghề đúng phạm vi chuyên môn đã được ghi trong giấy phép.',
            'Cung cấp tài liệu chứng minh trung thực, đầy đủ khi CareBridge yêu cầu đối soát.',
            'Không đang trong thời gian bị đình chỉ hành nghề hoặc bị cấm hành nghề.',
          ]}
        />
        <P>
          CareBridge có quyền từ chối hoặc thu hồi tư cách chuyên gia nếu phát hiện thông tin khai
          báo không chính xác, hoặc giấy phép hết hiệu lực mà không được cập nhật.
        </P>
      </>
    ),
  },
  {
    id: 'nghia-vu-chuyen-gia',
    title: 'Nghĩa vụ của chuyên gia',
    body: (
      <Bullets
        items={[
          <>
            <LawRef>Giữ bí mật thông tin người bệnh</LawRef> theo Điều 45 Luật Khám bệnh, chữa bệnh
            số 15/2023/QH15. Không tiết lộ, sao chép, sử dụng dữ liệu sức khỏe của người dùng cho mục
            đích ngoài phạm vi tư vấn.
          </>,
          'Tư vấn trung thực, dựa trên bằng chứng y khoa, không quảng cáo sai sự thật về hiệu quả điều trị.',
          'Không kê đơn thuốc kê toa qua nền tảng khi pháp luật yêu cầu phải khám trực tiếp.',
          'Hướng dẫn người dùng tới cơ sở y tế khi phát hiện dấu hiệu nguy hiểm vượt quá phạm vi tư vấn từ xa.',
          'Không sử dụng tài khoản của người khác hoặc cho người khác sử dụng tài khoản của mình.',
          'Cập nhật kịp thời khi giấy phép hành nghề thay đổi, gia hạn hoặc bị thu hồi.',
        ]}
      />
    ),
  },
  {
    id: 'gioi-han-dich-vu',
    title: 'Phạm vi và giới hạn của dịch vụ',
    body: (
      <>
        <P>
          CareBridge là nền tảng <LawRef>hỗ trợ kết nối và tư vấn từ xa</LawRef>. Dịch vụ trên nền
          tảng không thay thế việc khám bệnh, chữa bệnh trực tiếp tại cơ sở y tế và không thay thế
          dịch vụ cấp cứu.
        </P>
        <P>
          Trong tình huống nguy hiểm đến tính mạng, người dùng phải liên hệ ngay số cấp cứu{' '}
          <LawRef>115</LawRef> hoặc tới cơ sở y tế gần nhất. Tính năng hỗ trợ khẩn cấp trên CareBridge
          chỉ nhằm bổ trợ, không bảo đảm thay thế lực lượng cấp cứu chuyên nghiệp.
        </P>
      </>
    ),
  },
  {
    id: 'noi-dung',
    title: 'Nội dung và kiểm duyệt',
    body: (
      <>
        <P>
          Nội dung đăng tải trên cộng đồng CareBridge được sàng lọc tự động nhằm phát hiện thông tin y
          tế sai lệch, nội dung gây hại cho trẻ em, hành vi giả mạo chuyên gia và các nội dung bị cấm
          theo <LawRef>Luật An ninh mạng số 24/2018/QH14</LawRef>.
        </P>
        <P>
          Khi hệ thống sàng lọc không đưa ra được kết luận, nội dung được chuyển cho người kiểm duyệt
          xem xét trước khi hiển thị công khai. Nội dung vi phạm sẽ bị gỡ bỏ và tài khoản có thể bị
          hạn chế.
        </P>
        <P>
          Bạn giữ quyền sở hữu đối với nội dung mình đăng tải, đồng thời cấp cho CareBridge quyền sử
          dụng không độc quyền, miễn phí trong phạm vi cần thiết để vận hành và hiển thị dịch vụ.
        </P>
      </>
    ),
  },
  {
    id: 'hanh-vi-cam',
    title: 'Hành vi bị nghiêm cấm',
    body: (
      <Bullets
        items={[
          'Giả mạo danh tính, giả mạo giấy phép hành nghề hoặc mạo danh chuyên gia y tế.',
          'Thu thập, mua bán dữ liệu cá nhân của người dùng khác.',
          'Đăng tải thông tin y tế sai lệch có nguy cơ gây hại cho sức khỏe.',
          'Tấn công, dò quét, gây quá tải hoặc can thiệp trái phép vào hệ thống.',
          'Sử dụng dịch vụ cho mục đích vi phạm pháp luật Việt Nam.',
        ]}
      />
    ),
  },
  {
    id: 'dinh-chi',
    title: 'Đình chỉ và chấm dứt tài khoản',
    body: (
      <>
        <P>
          CareBridge có quyền tạm khóa hoặc chấm dứt tài khoản khi phát hiện vi phạm Điều khoản này,
          vi phạm pháp luật, hoặc khi cần bảo vệ an toàn cho người dùng khác.
        </P>
        <P>
          Bạn có thể yêu cầu chấm dứt tài khoản bất cứ lúc nào. Việc xóa dữ liệu sau khi chấm dứt được
          thực hiện theo Chính sách bảo mật, trừ phần dữ liệu phải lưu giữ theo yêu cầu của pháp luật.
        </P>
      </>
    ),
  },
  {
    id: 'trach-nhiem',
    title: 'Giới hạn trách nhiệm',
    body: (
      <>
        <P>
          Chuyên gia chịu trách nhiệm về nội dung tư vấn chuyên môn của mình theo quy định pháp luật
          về khám bệnh, chữa bệnh. CareBridge chịu trách nhiệm về việc vận hành nền tảng và bảo vệ dữ
          liệu theo Chính sách bảo mật.
        </P>
        <P>
          CareBridge không chịu trách nhiệm với thiệt hại phát sinh do người dùng cung cấp thông tin
          sai lệch, tự ý áp dụng lời khuyên ngoài phạm vi tư vấn, hoặc do sự cố từ hạ tầng viễn thông
          nằm ngoài kiểm soát hợp lý của nền tảng.
        </P>
      </>
    ),
  },
  {
    id: 'giai-quyet-tranh-chap',
    title: 'Luật áp dụng và giải quyết tranh chấp',
    body: (
      <P>
        Điều khoản này được điều chỉnh bởi pháp luật Việt Nam. Tranh chấp phát sinh trước hết được
        giải quyết thông qua thương lượng. Nếu không đạt được thỏa thuận, tranh chấp được đưa ra Tòa
        án nhân dân có thẩm quyền tại Việt Nam.
      </P>
    ),
  },
  {
    id: 'thay-doi-dieu-khoan',
    title: 'Thay đổi điều khoản',
    body: (
      <P>
        CareBridge có thể sửa đổi Điều khoản này. Bản sửa đổi được đăng tại trang này kèm ngày hiệu
        lực. Việc tiếp tục sử dụng dịch vụ sau ngày hiệu lực đồng nghĩa với việc bạn chấp nhận bản
        sửa đổi. Với thay đổi ảnh hưởng đáng kể tới quyền và nghĩa vụ, CareBridge sẽ thông báo trước
        trong ứng dụng.
      </P>
    ),
  },
  {
    id: 'lien-he',
    title: 'Liên hệ',
    body: (
      <P>
        Mọi thắc mắc về Điều khoản sử dụng xin gửi về <LawRef>{CONTACT_EMAIL}</LawRef>.
      </P>
    ),
  },
];

export default function TermsOfServicePage() {
  return (
    <LegalPageLayout
      documentTitle="Điều khoản sử dụng"
      subtitle="Điều kiện áp dụng khi bạn đăng ký và sử dụng CareBridge với tư cách chuyên gia y tế hoặc người dùng."
      sections={sections}
    />
  );
}
