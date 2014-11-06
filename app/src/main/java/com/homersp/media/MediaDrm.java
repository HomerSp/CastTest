package com.homersp.media;

import android.media.UnsupportedSchemeException;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.Cipher;

/**
 * Created by homer on 19/09/14.
 */
public class MediaDrm {
    private static final String TAG = "HomerSp." + MediaDrm.class.getSimpleName();

    private static final boolean ENABLE_OVERRIDE = true;

    private static final String RET_CERTIFICATEREQUEST_DEFAULTURL = "https://www.googleapis.com/certificateprovisioning/v1/devicecertificates/create?key=AIzaSyB-5OLKTx2iU5mko18DfdwK5611JIjbUhE";
    private static final String RET_CERTIFICATEREQUEST_DATA = "CmkKTAgAEkgAAAACAAAAeFq_F4UbMGIkn4SCuhONqeMCcpf4FtORgBlkc_hxOwgznasIlLGQE1dLh1snTwxloYRtbzxQYihGTDNoNToyaigSBLWCQoMaEwgBEg9jYXN0Lmdvb2dsZS5jb20SIEDpyLf9_oo88Tb6QTGk2xfa-zcLm7CmlJTkXuyCsUU3";

    private static final String RET_CERTIFICATERESPONSE_DATA = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUQ2akNDQXRLZ0F3SUJBZ0lFK08yalZEQU5C" +
            "Z2txaGtpRzl3MEJBUVVGQURCL01Rc3dDUVlEVlFRR0V3SlYKVXpFVE1CRUdBMVVFQ0F3S1YyRnph" +
            "R2x1WjNSdmJqRVJNQThHQTFVRUJ3d0lTMmx5YTJ4aGJtUXhFekFSQmdOVgpCQW9NQ2tkdmIyZHNa" +
            "U0JKYm1NeEVUQVBCZ05WQkFzTUNGZHBaR1YyYVc1bE1TQXdIZ1lEVlFRRERCZFhhV1JsCmRtbHVa" +
            "U0JOYjJ4c2VTQkRZWE4wSUVsRFFUQWVGdzB4TkRBNU1qWXdNVE0yTXpKYUZ3MHhOVEE1TWpZd01U" +
            "TTIKTXpKYU1Ic3hDekFKQmdOVkJBWVRBbFZUTVJNd0VRWURWUVFJREFwWFlYTm9hVzVuZEc5dU1S" +
            "RXdEd1lEVlFRSApEQWhMYVhKcmJHRnVaREVUTUJFR0ExVUVDZ3dLUjI5dloyeGxJRWx1WXpFUk1B" +
            "OEdBMVVFQ3d3SVYybGtaWFpwCmJtVXhIREFhQmdOVkJBTU1FekkwTWpnME5UVTFNamt6TURJNE1q" +
            "YzROakF3Z2dFaU1BMEdDU3FHU0liM0RRRUIKQVFVQUE0SUJEd0F3Z2dFS0FvSUJBUUNiZTFicUtu" +
            "c3F6K0F3TVVtOGkyZktvSks3K05DVkdnWmdDdUZKeTZRbQo1NTVsZFhQMkFiaWcyM2J4WmZPbHdw" +
            "ZXBRRzduekFEanYyQmFtVndNVkpXencrZjloSDhyUVU1cGJuNThjdVRRCk91OWpFOVhKYWhyakxO" +
            "TXpUV3FNVjBKSXJnQnhtd0NmdVFTTlgrSlFZSWhXbkNsZFdoSFRFVkVuMWxJRnNkQ2YKdnZwYm4y" +
            "S1lHN0gyOUhlNDFJUXdCL3R4OHpvZXNyUG81MHdaaXhJeFQwaXhzVk5UbEpDMlZNWXRmTk1kbUhi" +
            "dgovS1dsem5JUVFnaU56eUJHL3gwRU1jL3J3UVFqREtaeW9VWm9sN2VlOUh4L0VCa2ptcFN5L0Yv" +
            "RkRWdld2c1o3CldXYjBUNXdadGxYZTk4MTYvYi9JWVJwU0RGQmpCRG9xeVhiMElWL3dLRVJ6QWdN" +
            "QkFBR2pjakJ3TUF3R0ExVWQKRXdFQi93UUNNQUF3Q3dZRFZSMFBCQVFEQWdlQU1CTUdBMVVkSlFR" +
            "TU1Bb0dDQ3NHQVFVRkJ3TUNNQjBHQTFVZApEZ1FXQkJSTG9ucHlHbVgrL0xaV3dNOVZNSnNBd3hW" +
            "RlZUQWZCZ05WSFNNRUdEQVdnQlNEMm9DWUNRUndoTFQ5ClhXZGxRTDVRWElkd0Z6QU5CZ2txaGtp" +
            "Rzl3MEJBUVVGQUFPQ0FRRUFnQW9zRnZWdVE5YUdOU25tQ09Ma3lkbTEKbjBoY21KckVacmtRTlZ4" +
            "VHBsaFNTMm53TkxwdzFjNnJoVU5CNVI4YUZGblhwaGxKQmZiNHVNSW9MdUpSWWVtVApJbHJVT2pX" +
            "U1p6N0RmRUtvY1FjZGtHRE9xSmxWa3NZKzBOMS8wM3k2eDYrOERMZ0djTnU2VnpWaUZQWFlRajhi" +
            "Ckc0MUEvMFYwOTFyS1FnSHd2eTg0M1VxcDE2SStlZ1BKSURXSk5BN1pNWjcxMFYybm90OGdlbW1V" +
            "KzhMWVhvY2gKVTBWaGJrN3lVc3Z4cFNUWUtQV1IvcjBPa21VT2JKUzRlRTkzY3l2SmhhLzEvUHNs" +
            "Y3VRTnVuaGFPZjZUNmtKTApTczhVNlo1N0piRkRpWWU5dVZqSWl0NUNVaGtSb1czN0VQZXNIcXVR" +
            "Y1RReDRLOFUvN3VvdHY2eHBFbWtpQT09Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0KLS0tLS1C" +
            "RUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUR6VENDQXJXZ0F3SUJBZ0lCQVRBTkJna3Foa2lHOXcw" +
            "QkFRVUZBREI5TVFzd0NRWURWUVFHRXdKVlV6RVQKTUJFR0ExVUVDQXdLVjJGemFHbHVaM1J2YmpF" +
            "Uk1BOEdBMVVFQnd3SVMybHlhMnhoYm1ReEV6QVJCZ05WQkFvTQpDa2R2YjJkc1pTQkpibU14RVRB" +
            "UEJnTlZCQXNNQ0ZkcFpHVjJhVzVsTVI0d0hBWURWUVFEREJWWGFXUmxkbWx1ClpTQkRZWE4wSUZO" +
            "MVluSnZiM1F3SGhjTk1UUXdOREE1TVRjMU56RTBXaGNOTVRrd05EQTRNVGMxTnpFMFdqQi8KTVFz" +
            "d0NRWURWUVFHRXdKVlV6RVRNQkVHQTFVRUNBd0tWMkZ6YUdsdVozUnZiakVSTUE4R0ExVUVCd3dJ" +
            "UzJseQphMnhoYm1ReEV6QVJCZ05WQkFvTUNrZHZiMmRzWlNCSmJtTXhFVEFQQmdOVkJBc01DRmRw" +
            "WkdWMmFXNWxNU0F3CkhnWURWUVFEREJkWGFXUmxkbWx1WlNCTmIyeHNlU0JEWVhOMElFbERRVEND" +
            "QVNJd0RRWUpLb1pJaHZjTkFRRUIKQlFBRGdnRVBBRENDQVFvQ2dnRUJBTGZvdytRczNuUlQ4a21W" +
            "YmRIYWFWY05odVh0dExubWM1OXNyVHRraFFNTgpDRVN2R0dtQ3JhbDBaRGRINGVjbUdUTTg0dEMx" +
            "aER6WHJHT3V4RElqOXR3VUVFdVZmK2lZMTNxZVF6MW9peXB3Cjl4NURjTHFscFpPdGl0U2ZySU1X" +
            "ODBoZnhlQ2xSTGhQMmRoMWtDV0w0eHhzMm9qL0NTdktIa2pkZGc5b1Zuc1YKbmNwckhQZEl3b25H" +
            "a3dveDhuZ25SVDN4RFZ0dVZUTHZTYURXcjZZd2tmSWhMOXVrS2JtYklyek5DNmFMcGlKNQovYytW" +
            "azVhekk4bkdNSTdBNlIvcysvV0kzWmR5RmlrSStrTG5UOHF1MXc4alNKdUNwemRLM1dBRWRkemVD" +
            "WmpTCkZpTUVjRTJabjBxQ0tPYStqNTIvb1V1aXV2V3lVUjVPNTRDZWVqaWh4d2tDQXdFQUFhTldN" +
            "RlF3RWdZRFZSMFQKQVFIL0JBZ3dCZ0VCL3dJQkFEQWRCZ05WSFE0RUZnUVVnOXFBbUFrRWNJUzAv" +
            "VjFuWlVDK1VGeUhjQmN3SHdZRApWUjBqQkJnd0ZvQVU1bGZRV3N6bHQ0MkRBYUZZMkVWMnpZM1lO" +
            "TUl3RFFZSktvWklodmNOQVFFRkJRQURnZ0VCCkFDb1RuczVtQ0dsVGVadk1KNkdGNnZabTZnN3oz" +
            "VnoyK0g3YWthWFBkOERZOFk3bmdPYUNmZVNYbXR4Wi9TL20KZm5POWY1VWpPcWptbkhSSlFvaUNm" +
            "SENBODlGZFNNUXNmNW8xaVNyZGVnc00yaE1QM3hOWW5iZVR5Wk5WaWZjagpBSm5lNE9YU2JMSEV1" +
            "N1hlcjFiT0tBeEtnMlVpajNoSFBxUVJQRTNBU1hBOFJLWG5RbnJQclg0Wjl6aVlTMzdXCmtFRVNO" +
            "bEdiOXVjK3ZBMXhWMzBvWHJBVnM0UjI5RHByTWlvVHIvT3pKUWdFMmtUWTc0NHF2Z1EzWHlxRkpG" +
            "NXoKUUYyNFlYOGRQOGpKcUFjOUNQblhXVnNHbk8yVm1kZ2NVUXdZQmZ1Ymkyd3NQVThPSmJOMXVP" +
            "Qi85SmVGaDM2RwoyWWJyZThRdXpTV0ttcGxQWGdOZ1FrVT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUt" +
            "LS0tLQotLS0tLUJFR0lOIENFUlRJRklDQVRFLS0tLS0KTUlJRHpUQ0NBcldnQXdJQkFnSUJCVEFO" +
            "QmdrcWhraUc5dzBCQVFVRkFEQjFNUXN3Q1FZRFZRUUdFd0pWVXpFVApNQkVHQTFVRUNBd0tRMkZz" +
            "YVdadmNtNXBZVEVXTUJRR0ExVUVCd3dOVFc5MWJuUmhhVzRnVm1sbGR6RVRNQkVHCkExVUVDZ3dL" +
            "UjI5dloyeGxJRWx1WXpFTk1Bc0dBMVVFQ3d3RVEyRnpkREVWTUJNR0ExVUVBd3dNUTJGemRDQlMK" +
            "YjI5MElFTkJNQjRYRFRFME1EUXdPVEExTVRJeU1Wb1hEVEU1TURRd09UQTFNVEl5TVZvd2ZURUxN" +
            "QWtHQTFVRQpCaE1DVlZNeEV6QVJCZ05WQkFnTUNsZGhjMmhwYm1kMGIyNHhFVEFQQmdOVkJBY01D" +
            "RXRwY210c1lXNWtNUk13CkVRWURWUVFLREFwSGIyOW5iR1VnU1c1ak1SRXdEd1lEVlFRTERBaFhh" +
            "V1JsZG1sdVpURWVNQndHQTFVRUF3d1YKVjJsa1pYWnBibVVnUTJGemRDQlRkV0p5YjI5ME1JSUJJ" +
            "akFOQmdrcWhraUc5dzBCQVFFRkFBT0NBUThBTUlJQgpDZ0tDQVFFQXA0dEhEN0dTclZETlRjV3RE" +
            "NGFQTjNzMktCVmJDUU9CQk44VUlrMGZFMVpMSERPQ05oZUxUa2tkCi8rOFlMYmRVNnpyNzNSWjZU" +
            "RHpvRzVRSTk4RFgvUzNjT1ZpN1NucGhjTDNwYlpzSE9JOFBkRVUydEJlTUdWRXYKL3pvSkJiV0lO" +
            "aEgyeGV5UER4MnpXa0t3anl5eW51MjZjWnFCYU5rVWpjdnNBOGtTb2NGTUF4ZGlYK2Q3VGY4dQpS" +
            "U3FaM29lVzNyeHgydVBsVmw3VkpGbmVVVVZFUVRUZmUyODRtZ055eUg3bmxCSzltc0EzT0lVNGt6" +
            "OGlqRk5ECmhEdzQwN1VLOTBjRFVkS0pTMjEvc2tjSVhaWTQzOUNIMHpNcVUxTEptNW5RZzNmK0Ri" +
            "MmVZcU1obk11NnRmWmMKL3Z4UzhmLzJNUlhJeWs2czNJcytoQUhNSHZualR3SURBUUFCbzJBd1hq" +
            "QVBCZ05WSFJNRUNEQUdBUUgvQWdFQgpNQjBHQTFVZERnUVdCQlRtVjlCYXpPVzNqWU1Cb1ZqWVJY" +
            "Yk5qZGcwd2pBZkJnTlZIU01FR0RBV2dCUjhtaDU5CjMzbFV2TmZNWHNxWmhrVjVaWFFvR1RBTEJn" +
            "TlZIUThFQkFNQ0FRWXdEUVlKS29aSWh2Y05BUUVGQlFBRGdnRUIKQUp5OXVXSnNJQVJGaWlMYXBh" +
            "U01lRGR3a3ArVHFEUUxhckcvelVZRjZWTHAzenp3dFlTcXEzSTlnRTNEaW1KdApKbHZHR2E4R2VW" +
            "dGJxTm9HRGFHQXZWaDdnNENtQ200aWhZVS95bXZTOVNiOFcrTm5ybHdXQ3lhL3NzYUdhUU9TCmda" +
            "WnRJckl5U3VQMy8zN092MlpvMS81UlVSZmErWStuM1lKUzUvLzhsbmxZRkhRNnJ3aEFjNlR4cWEr" +
            "NDUrNVYKeFN2eUt6MmQ3N0lpWlR5MzNJUjhieXVXSWRuSVRQUnkxSEZ5WWg5TVpOZ0k1MGJjRzZF" +
            "YTRwbndFd1ByUTV1QQp4MHNSMXNJaEdYMjFqTnhmdmtpVDU5QnFLOUNFSWZzQzV3T1NjaDI3cWdy" +
            "VUpLMnRDUEFVYWhJenl1MHdmNTBSClVLcnRjaklTR2x2TGc0SGQwYXZ6bEdFPQotLS0tLUVORCBD" +
            "RVJUSUZJQ0FURS0tLS0tCi0tLS0tQkVHSU4gQ0VSVElGSUNBVEUtLS0tLQpNSUlEeFRDQ0FxMmdB" +
            "d0lCQWdJQkFqQU5CZ2txaGtpRzl3MEJBUVVGQURCMU1Rc3dDUVlEVlFRR0V3SlZVekVUCk1CRUdB" +
            "MVVFQ0F3S1EyRnNhV1p2Y201cFlURVdNQlFHQTFVRUJ3d05UVzkxYm5SaGFXNGdWbWxsZHpFVE1C" +
            "RUcKQTFVRUNnd0tSMjl2WjJ4bElFbHVZekVOTUFzR0ExVUVDd3dFUTJGemRERVZNQk1HQTFVRUF3" +
            "d01RMkZ6ZENCUwpiMjkwSUVOQk1CNFhEVEUwTURRd01qRTNNelF5TmxvWERUTTBNRE15T0RFM016" +
            "UXlObG93ZFRFTE1Ba0dBMVVFCkJoTUNWVk14RXpBUkJnTlZCQWdNQ2tOaGJHbG1iM0p1YVdFeEZq" +
            "QVVCZ05WQkFjTURVMXZkVzUwWVdsdUlGWnAKWlhjeEV6QVJCZ05WQkFvTUNrZHZiMmRzWlNCSmJt" +
            "TXhEVEFMQmdOVkJBc01CRU5oYzNReEZUQVRCZ05WQkFNTQpERU5oYzNRZ1VtOXZkQ0JEUVRDQ0FT" +
            "SXdEUVlKS29aSWh2Y05BUUVCQlFBRGdnRVBBRENDQVFvQ2dnRUJBTHJaClpaM2FPZFBCZC9iVTBL" +
            "NlBXQWhvT1VxVjdYRFAvWGtJcWFybDZiaW5MYUJuUjRxZXljOXdzd1dIYVJIc2NKaVgKdytiRHcr" +
            "dTl4ckE5L0UvQlhqaWYyczl6TUFaYmVUZkJYb3lIUjVTYVFaSXExcFhFY1Z3blhRaXhnTWFTdlJ2" +
            "agpRWmVoN0hXZlZaNCtuNDhjeDJWa0I5T3pscUVFbjVIRTNncDdiTm5Jd0hneG9CbENxZWlENDg3" +
            "ODhjN0NMaVJHCmxRa1p5c0JHc3VVQnV0ZFA4Ny8yYWEyWkJQcWdCemtPNXQ5UlJ3ZkE1S2xjUzVU" +
            "Rkw3T2dNSC9ubFd1eXJ6SU4KOFl6VmJjdDdSNmNJcThzbm8wM1BTbHJ4QmRINFlzVVFLblJwcXVa" +
            "TGx2dWIyR1BrV0diVHJZcHUvM3RlK2FWVwpIaTJDTVZ2dzRpVG1RVW9mcmhNQ0F3RUFBYU5nTUY0" +
            "d0R3WURWUjBUQkFnd0JnRUIvd0lCQWpBZEJnTlZIUTRFCkZnUVVmSm9lZmQ5NVZMelh6RjdLbVla" +
            "RmVXVjBLQmt3SHdZRFZSMGpCQmd3Rm9BVWZKb2VmZDk1Vkx6WHpGN0sKbVlaRmVXVjBLQmt3Q3dZ" +
            "RFZSMFBCQVFEQWdFR01BMEdDU3FHU0liM0RRRUJCUVVBQTRJQkFRQ0E5RnI3UFNnWgpVU0RYMVBz" +
            "U2wwcGw4bGcxa25jd2F2SFh0bEVhZjVyTngzc0RRcTFWYWdDdjhPRUd3cjFyZUhYYi9rRVJVMG81" +
            "CnU1bzZ4bGswTHl3ejQ3TFdYSC9kZU90eFd6bmFnNURGTWVJL0krL2E2eXN0ZDE3ZXcwUFN5V3Ra" +
            "Z3NyVjdmcWgKWkZ2TDhRMGFZdUdjNktjWWNQQmZGNWI0N1liYnJoM2d6ejVkTHU0V2JaVXJQUDJY" +
            "OHdWYUpHaE5PYmI0NUZpNgo5ZUFtZUZIRlcxMU9DZVZzUjR0NldpNkpVK2JNTmxzbVBQaHlRd0tD" +
            "MGl2TjhOT2o3Qk0rVXRXRFBRZmNIVU5sCmVqTUNBYVBPdDlaZ1VUc0p3aU9LTXY2WUdXQmlrNFhO" +
            "TkViYjFTTVBlZHAzQUNvQ2JZTll6Z04zTmVHaklKUEMKU3FLa1JoeDFMQjlOCi0tLS0tRU5EIENF" +
            "UlRJRklDQVRFLS0tLS0K";
    private static final String RET_CERTIFICATERESPONSE_KEY = "fZYgWLA4UZvLpzxki0/HCORU6Rip1P/kH49dQ5MH02LliiD1zbaziC/h+kDz1tpGk/zFCvkLMKrc" +
            "TBESrVZ4OagGGUk2HCEyVraotZZm/qvmOl1VciIB9a79XQZ2f8ZW9yRgD+2v25CkrfCirMUlUETL" +
            "JyIoBM8oFNCTsYw/u5KbeiFWuHeVhjuvG8gCHCjPpTwDqe85ZsIqas5j53VGkPeG8h5CZRG+PLFy" +
            "q4hKUiACyHEG8QXQ/RHI6y9pFXhaQu2l0/7EvKy9sYS1a7NrDZm58/GTJl51Wyuuo9QUKSrtnQRe" +
            "MAdWY/Gqv/13lgLKLwkCoZU4vgtyl+71vaW1uSR71+AJj9xfO1c5UPIIYX+iyHaARqwRFBjZ77vq" +
            "ErpWRHTsTNSnuRZXfliLu9mM+2hiQ/mEfxAcjwNLidtQ/v0DdFreZaToF2bzd4+E/pl6khYqHg/l" +
            "ks6lyo7EvGA2PAr0feZA8Q8rfGdRsF2nsMlSflWUmLpg25Xv5o/MpckYOGdhN7RdYrdJxZKBWiSE" +
            "hcx6pk0VtxpsLguICbzNa/IjOxyU/uqiqbzDgheC4U/fiEglDKsUymMFzEUesiDCX4/KQh/+SU/V" +
            "fcUO02fiF453yPQk4B1aMgFHzlIxu9OITaceJYobHRh26rbKeUn8t0xmflrIuMHjctUvBspLkQeK" +
            "d5ad0NkBSKsyUDvkrK3jecqiEgG8KVqqjAgzXkCASLooY4Z5Z9xVKmsiIwRaqsaJuCPOY581XmdU" +
            "M/2CDhMCrlymENB8xkqhL/ZfHrHWNgu6THn0biz6ho6Ic9/NqCtclEvAukNFidelCEon6muPCww4" +
            "yZWiI94QrY250Mr7O5QqIixHEUhuV3GjWjJip3axHWCTOzzmQWYzeQXUmXP74Yo1coMHluytzzlx" +
            "+tgfkWv1wVqQscMgfOVmpxcX6a1EQPuaCK3TTqL68HCSAumanAchNfV8i296Zrum2yVR4xz1CuIv" +
            "5K9JwpyQIp4nJ2z8D6PR78HlgLzqBFHj+QFjEgxinqdXaP6EyqpB04ZNwx+R1qXTauVwf0GUXWqC" +
            "U2VfkQAY5RvBGqwpcTH5+CN/h5PioXCP9UZByHoryyXH8bsyOZAtZt7rJ/xoJVbqX8V8iDLb5km4" +
            "OMoEE8aarywaXJ0iHbOhGi60gEHe4OJaqP4rD4SwLWjQKEBxEY22hRh+Z3tbr/kUDGO4BXKFgZoE" +
            "Kq3bkeI9llcgYkweC/KwN/HLe4p5nArfJXzr8GY/stdE6HlohwPfRB/2fKN3eWPpI9t27Bnikczc" +
            "w5SxURN/D4v4vIx6hcmQrAyvcFi9o7RtUi2tipfxarKalUXi9XdJdLYJ+ouaK1GQyLc+DXHgusMO" +
            "0cGaAMhtw7xETchlD5Ue3UQCixYIFduVQuYWlJBkOiI2zFdS90q/Midd9d77ckRk6lKB1CjtCkes" +
            "OTNrHXRVE2ojmasrM2PW0n9T8gbjxojv8fXwa8Vrl/7nMF3ahBpG+j1EZX70JZUntb2IbWxL5Tmg" +
            "FVHNXLda4vfugdkna4oYdbbd546mTpu6M7tBzlUJcsZOZN1IAG8wjxUdef4mUbXWsEzJWOWxtuMk" +
            "dA+S8uOIPtEeFwnyU+n1Zi3GseZbHnw8RvJLPImWzYlnkiNkvSBHljNDk6PNPq5mbp3h";

    private static final String RET_SIGNRSA_DATA = "HhRVl5pbioMKa47Fdpyuavi3LeP/sH8vcV9lf0rM04U7u3OvUrnBZr+quM/3Zw++04esIa55kl7z" +
            "7B6mhUjI6tW1u0wEXqNoK9GPC9lplQjeGFheCqgW0IQiS4z7x3Umbq+JAtOQYFcuD/BBcVMhhBcv" +
            "QUheL3K6TVcS92khDmLBPgc5K5Rl4RlQINbExf3a85T9F+gN8x3l1DdAhbHPs5My0ZXNZvtSuO5x" +
            "nsFIcDpyItse4lgjapyeg0tBlrkFSh85k50LNoYGlH9d7MXPAWjcdtl4GaC4SpD/5zqG7nStrlQa" +
            "osQALie/+8v8xahld5MB7OyAkJydlpSxB88xoQ==";

    public static android.media.MediaDrm createMediaDrm()
    {
        UUID wvUUID = new UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L);
        try {
            return new android.media.MediaDrm(wvUUID);
        } catch (UnsupportedSchemeException e) {
            //e.printStackTrace();
        }

        return null;
    }

    public static MediaDrm.CertificateRequest getCertificateRequest(android.media.MediaDrm drm, int certType, String certAuthority) {
        try {
            MediaLogger.instance().log("getCertificateRequest start type", String.valueOf(certType));
            MediaLogger.instance().log("getCertificateRequest start authority", certAuthority);
        } catch(Throwable tr) {

        }

        byte[] data;
        String defaultUrl;

        if(!ENABLE_OVERRIDE) {
            try {
                Class<?> mhn = drm.getClass();
                Method method = mhn.getDeclaredMethod("getCertificateRequest", Integer.TYPE, String.class);
                method.setAccessible(true);
                Object certRequest = method.invoke(drm, certType, certAuthority);

                Class<?> certRequestClass = Class.forName("android.media.MediaDrm$CertificateRequest");

                Field dataField = certRequestClass.getDeclaredField("mData");
                dataField.setAccessible(true);

                Field defaultUrlField = certRequestClass.getDeclaredField("mDefaultUrl");
                defaultUrlField.setAccessible(true);

                data = (byte[]) dataField.get(certRequest);
                defaultUrl = (String) defaultUrlField.get(certRequest);
            } catch (Throwable e) {
                defaultUrl = RET_CERTIFICATEREQUEST_DEFAULTURL;
                data = Base64.decode(RET_CERTIFICATEREQUEST_DATA, 0);
            }
        } else {
            defaultUrl = RET_CERTIFICATEREQUEST_DEFAULTURL;
            data = Base64.decode(RET_CERTIFICATEREQUEST_DATA, 0);
        }

        MediaLogger.instance().log("getCertificateRequest ret defaultUrl", defaultUrl);
        MediaLogger.instance().log("getCertificateRequest ret data", Base64.encodeToString(data, 0));

        return new com.homersp.media.MediaDrm.CertificateRequest(data, defaultUrl);
    }

    public static com.homersp.media.MediaDrm.Certificate provideCertificateResponse(android.media.MediaDrm drm, byte[] response) {
        try {
            MediaLogger.instance().log("provideCertificateResponse start response", Base64.encodeToString(response, 0));
        } catch(Throwable tr) {

        }

        byte[] certData, wrappedKey;

        if(!ENABLE_OVERRIDE) {
            try {
                Class<?> mhn = drm.getClass();
                Method method = mhn.getDeclaredMethod("provideCertificateResponse", byte[].class);
                method.setAccessible(true);

                Object obj = method.invoke(drm, new Object[] {response});

                Class<?> certClass = Class.forName("android.media.MediaDrm$Certificate");

                Field certificateDataField = certClass.getDeclaredField("mCertificateData");
                certificateDataField.setAccessible(true);

                Field wrappedKeyField = certClass.getDeclaredField("mWrappedKey");
                wrappedKeyField.setAccessible(true);

                certData = (byte[]) certificateDataField.get(obj);
                wrappedKey = (byte[]) wrappedKeyField.get(obj);
            } catch (Throwable e) {
                certData = Base64.decode(RET_CERTIFICATERESPONSE_DATA, 0);
                wrappedKey = Base64.decode(RET_CERTIFICATERESPONSE_KEY, 0);
            }
        } else {
            certData = Base64.decode(RET_CERTIFICATERESPONSE_DATA, 0);
            wrappedKey = Base64.decode(RET_CERTIFICATERESPONSE_KEY, 0);
        }

        MediaLogger.instance().log("provideCertificateResponse ret data", Base64.encodeToString(certData, 0));
        MediaLogger.instance().log("provideCertificateResponse ret wrappedKey", Base64.encodeToString(wrappedKey, 0));

        return new Certificate(certData, wrappedKey);
    }

    public static byte[] signRSA(android.media.MediaDrm drm, byte[] sessionId, String algorithm, byte[] wrappedKey, byte[] message) {
        try {
            MediaLogger.instance().log("signRSA start sessionId", new String(sessionId));
            MediaLogger.instance().log("signRSA start algorithm", algorithm);
            MediaLogger.instance().log("signRSA start wrappedKey", Base64.encodeToString(wrappedKey, 0));
            MediaLogger.instance().log("signRSA start message", Base64.encodeToString(message, 0));
        } catch(Throwable tr) {

        }

        byte []data;
        if(!ENABLE_OVERRIDE) {
            try {
                Class<?> mhn = drm.getClass();
                Method method = mhn.getDeclaredMethod("signRSA", byte[].class, String.class, byte[].class, byte[].class);
                method.setAccessible(true);
                data = (byte[]) method.invoke(drm, sessionId, algorithm, wrappedKey, message);
            } catch (Throwable e) {
                data = Base64.decode(RET_SIGNRSA_DATA, 0);
            }
        } else {
            data = Base64.decode(RET_SIGNRSA_DATA, 0);
        }

        MediaLogger.instance().log("signRSA ret data", Base64.encodeToString(data, 0));

        return data;
    }

    public static final class Certificate {
        private byte[] mCertificateData;
        private byte[] mWrappedKey;

        public Certificate(byte[] certData, byte[] wrappedKey)
        {
            mCertificateData = certData;
            mWrappedKey = wrappedKey;
        }

        public byte[] getContent() {
            return mCertificateData;
        }
        public byte[] getWrappedPrivateKey() {
            return mWrappedKey;
        }
    }

    public static final class CertificateRequest {
        private byte[] mData;
        private String mDefaultUrl;

        public CertificateRequest(byte[] data, String defaultUrl) {
            mData = data.clone();
            mDefaultUrl = defaultUrl;
        }

        public byte[] getData() {
            return mData;
        }
        public String getDefaultUrl() {
            return mDefaultUrl;
        }
    }
}
