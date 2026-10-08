const dishes=[
{id:1,cat:"starter",name:"کارپاچیو ریورینا",en:"Riverina Carpaccio",desc:"گوشت ورقه‌ای، روغن زیتون، لیمو، سبزی‌های معطر و پارمزان.",price:420000,img:"https://media.alotea.com/rooster-and-owl-washington-cover.webp",badge:"POPULAR"},
{id:2,cat:"starter",name:"تارت ماکادمیا",en:"Macadamia Tart",desc:"تارت ظریف ماکادمیا، سبزی‌های فصلی و پنیر نرم.",price:360000,img:"https://www.fourseasons.com/content/dam/fourseasons/images/web/MEX/MEX_1687_aspect16x9.jpg"},
{id:3,cat:"main",name:"استیک گریل زغالی",en:"Charcoal Ribeye",desc:"ریب‌آی فصلی، سس سبز، سبزیجات کبابی و کره معطر.",price:890000,img:"https://cms.marivalresorts.com/storage/images/gallery/HAOMExl9NnAid9GqcV8btIBRdr3S403WoHcqXkas.jpg",badge:"CHEF'S PICK"},
{id:4,cat:"main",name:"بیف بریزد",en:"Durif Braised Beef",desc:"بیف آرام‌پز، پوره سیر، سبزی‌های برگ‌دار و سالسا ورد.",price:780000,img:"https://images.squarespace-cdn.com/content/v1/5cc688b151f4d415ffc1491f/1712051963938-DKCSNW2LC5978ST69VW0/Thecharles_-35.jpg"},
{id:5,cat:"main",name:"ماهی روز",en:"Catch of the Day",desc:"ماهی تازه روز با سس مرکبات و سبزیجات معطر.",price:690000,img:"https://d3h1lg3ksw6i6b.cloudfront.net/media/image/2026/03/25/88a250fe10614b008de0048224941414_2210-london.jpg"},
{id:6,cat:"main",name:"مرغ پرتقالی",en:"Orange Glazed Chicken",desc:"مرغ برشته، سس پرتقال، کینوا و سبزی تازه.",price:590000,img:"https://www.mishmish.cz/assets/service-fb-concept-BfzIGlzV.jpg"},
{id:7,cat:"dessert",name:"شکلات مارکیز",en:"Chocolate Marquise",desc:"شکلات تلخ، خامه زنجبیلی، ریواس و ژله تونیک.",price:340000,img:"https://symphony.cdn.tambourine.com/quail-lodge-golf/media/all-in-carousel_--food-6961759af128b.jpg",badge:"SIGNATURE"},
{id:8,cat:"dessert",name:"لِمون تارت",en:"Lemon Tart",desc:"تارت لیمو، کمپوت مرکبات و خامه اسطوخودوس.",price:290000,img:"https://offloadmedia.feverup.com/santiagosecreto.com/wp-content/uploads/2023/03/08104125/DoDoh_foto01.png"},
{id:9,cat:"drink",name:"امضای روبی",en:"Ruby Signature",desc:"کوکتل بدون الکل انار، مرکبات و رزماری.",price:220000,img:"https://www.gemdanismanlik.com/uploads/images/gemdanismanlik.jpg"},
{id:10,cat:"drink",name:"سیتروس فیز",en:"Citrus Fizz",desc:"مرکبات تازه، تونیک و لایه معطر گیاهی.",price:210000,img:"https://images.squarespace-cdn.com/content/v1/5cc688b151f4d415ffc1491f/1775817197957-D716MRNFJ3S4VP6XNM0S/Menu%2B%26%2BRestaurant%2B-%2BThe%2BCharles%2Bx%2BThat%2BGreen%2BOlive%2B118.jpg"},
{id:11,cat:"starter",name:"فوکاشیا خانگی",en:"House Focaccia",desc:"خمیر تخمیرشده، روغن زیتون و نمک دریا.",price:190000,img:"https://images.squarespace-cdn.com/content/v1/5cc688b151f4d415ffc1491f/1712051627162-REBL2G085B85N1E6K0S7/Thecharles_-76.jpg"},
{id:12,cat:"main",name:"دو پرنده",en:"Duck Two Ways",desc:"اردک برشته و آرام‌پز با سس میوه‌ای فصلی.",price:820000,img:"https://godomall-storage.cdn-nhncommerce.com/e9f3698822f4752682e9e476d8056c3d/goods/9240351/image/main/9240351_Main.jpg"}
];

const $=s=>document.querySelector(s), $$=s=>document.querySelectorAll(s), fa=n=>Number(n).toLocaleString("fa-IR"), money=n=>fa(n)+" تومان";
let filter="all", cart=JSON.parse(localStorage.getItem("restaurantCart")||"[]");

window.addEventListener("load",()=>setTimeout(()=>$("#preloader").classList.add("hidden"),400));
window.addEventListener("scroll",()=>$("#header").classList.toggle("scrolled",scrollY>35));

function renderMenu(){
 const q=($("#menuSearch").value||"").trim().toLowerCase();
 const arr=dishes.filter(d=>(filter==="all"||d.cat===filter)&&(!q||[d.name,d.en,d.desc].join(" ").toLowerCase().includes(q)));
 $("#menuGrid").innerHTML=arr.length?arr.map(d=>"<article class='dish-card'><button class='dish-media' data-dish='"+d.id+"' aria-label='جزئیات "+d.name+"'><img src='"+d.img+"' alt='"+d.name+"' loading='lazy'>"+(d.badge?"<span class='dish-badge'>"+d.badge+"</span>":"")+"</button><div class='dish-body'><div class='dish-top'><div><h3>"+d.name+"</h3><small style='color:#777;font-size:9px'>"+d.en+"</small></div><span class='price'>"+money(d.price)+"</span></div><p>"+d.desc+"</p><div class='dish-actions'><button class='btn btn--ghost add-cart' data-add='"+d.id+"'>افزودن به سفارش</button><button class='btn btn--dark' data-dish='"+d.id+"'>جزئیات</button></div></div></article>").join(""):"<div style='grid-column:1/-1;text-align:center;padding:50px;color:#777'>موردی پیدا نشد.</div>";
}

function saveCart(){localStorage.setItem("restaurantCart",JSON.stringify(cart));renderCart();updateCartCount()}
function updateCartCount(){$("#cartCount").textContent=fa(cart.reduce((s,x)=>s+x.qty,0))}
function addCart(id){const d=dishes.find(x=>x.id===id),x=cart.find(v=>v.id===id);x?x.qty++:cart.push(Object.assign({},d,{qty:1}));saveCart();toast("به سفارش اضافه شد")}
function renderCart(){const el=$("#cartItems");if(!cart.length){el.innerHTML="<div class='cart-empty'>هنوز چیزی انتخاب نکرده‌اید.</div>";$("#cartTotal").textContent="۰ تومان";return}el.innerHTML=cart.map(x=>"<div class='cart-item'><img src='"+x.img+"' alt='"+x.name+"'><div><h4>"+x.name+"</h4><small>"+money(x.price)+"</small><div class='cart-controls'><button data-dec='"+x.id+"'>−</button><span>"+fa(x.qty)+"</span><button data-inc='"+x.id+"'>+</button></div></div><strong style='color:#c6a15b;font-size:11px'>"+money(x.price*x.qty)+"</strong></div>").join("");$("#cartTotal").textContent=money(cart.reduce((s,x)=>s+x.price*x.qty,0))}
function lock(){document.body.classList.add("lock")} function unlock(){document.body.classList.remove("lock")}
function openModal(id){$(id).classList.add("is-open");$(id).setAttribute("aria-hidden","false");lock()} function closeAll(){$$(".modal,.drawer,.search-overlay").forEach(x=>{x.classList.remove("is-open");x.setAttribute("aria-hidden","true")});unlock()}
function toast(m){const t=$("#toast");t.textContent=m;t.classList.add("show");clearTimeout(window.__t);window.__t=setTimeout(()=>t.classList.remove("show"),2200)}
function showDish(id){const d=dishes.find(x=>x.id===id);$("#dishModalContent").innerHTML="<div class='dish-detail'><img src='"+d.img+"' alt='"+d.name+"'><div><p class='eyebrow'>"+d.en+"</p><h2>"+d.name+"</h2><p>"+d.desc+"</p><div class='dish-detail__meta'><strong>"+money(d.price)+"</strong><span>•</span><span>پیشنهاد سرآشپز</span></div><button class='btn btn--gold btn--full' data-add='"+d.id+"'>افزودن به سفارش</button></div></div>";openModal("#dishModal")}
function globalSearch(q){const r=$("#globalSearchResults"),arr=dishes.filter(d=>[d.name,d.en,d.desc].join(" ").toLowerCase().includes(q.toLowerCase())).slice(0,7);r.innerHTML=arr.map(d=>"<a class='search-result' href='#menu' data-jump='"+d.id+"'><span><b>"+d.name+"</b><small> — "+d.en+"</small></span><strong>"+money(d.price)+"</strong></a>").join("")||"<p style='color:#777'>نتیجه‌ای پیدا نشد.</p>"}

$("#menuTabs").addEventListener("click",e=>{const b=e.target.closest("[data-filter]");if(!b)return;$$("[data-filter]").forEach(x=>x.classList.remove("active"));b.classList.add("active");filter=b.dataset.filter;renderMenu()});
$("#menuSearch").addEventListener("input",renderMenu);
document.addEventListener("click",e=>{
 const add=e.target.closest("[data-add]");if(add){addCart(Number(add.dataset.add));return}
 const dish=e.target.closest("[data-dish]");if(dish){showDish(Number(dish.dataset.dish));return}
 if(e.target.closest("[data-close]")||e.target.closest("[data-close-drawer]")){closeAll();return}
 const inc=e.target.closest("[data-inc]");if(inc){const x=cart.find(v=>v.id===Number(inc.dataset.inc));x.qty++;saveCart();return}
 const dec=e.target.closest("[data-dec]");if(dec){const x=cart.find(v=>v.id===Number(dec.dataset.dec));x.qty--;if(x.qty<=0)cart=cart.filter(v=>v.id!==x.id);saveCart();return}
 const jump=e.target.closest("[data-jump]");if(jump){closeAll();setTimeout(()=>showDish(Number(jump.dataset.jump)),80)}
});
$("#openCart").addEventListener("click",()=>{$("#cartDrawer").classList.add("is-open");$("#cartDrawer").setAttribute("aria-hidden","false");lock()});
$("#fakeCheckout").addEventListener("click",()=>toast("درگاه پرداخت در این نسخه متصل نیست."));
$("#openSearch").addEventListener("click",()=>{$("#searchOverlay").classList.add("is-open");$("#searchOverlay").setAttribute("aria-hidden","false");lock();setTimeout(()=>$("#globalSearch").focus(),50)});
$("#closeSearch").addEventListener("click",closeAll);
$("#globalSearch").addEventListener("input",e=>globalSearch(e.target.value));
$("#reservationForm").addEventListener("submit",e=>{e.preventDefault();const d=new FormData(e.target);$("#reservationStatus").textContent="درخواست نمایشی "+d.get("name")+" برای "+d.get("guests")+" در تاریخ "+d.get("date")+" ساعت "+d.get("time")+" ثبت شد.";e.target.reset();toast("رزرو ثبت شد")});
$("#newsletterForm").addEventListener("submit",e=>{e.preventDefault();e.target.reset();toast("ایمیل شما ثبت شد.")});
$$(".gallery-item").forEach(x=>x.addEventListener("click",()=>{$("#modalImage").src=x.dataset.full;$("#modalCaption").textContent="Restaurant Gallery";openModal("#imageModal")}));
$("#menuToggle").addEventListener("click",()=>{$(".main-nav").classList.toggle("mobile-open")});
$$(".reveal").forEach(el=>new IntersectionObserver(es=>es.forEach(v=>v.isIntersecting&&v.target.classList.add("visible")),{threshold:.12}).observe(el));
$$("[data-counter]").forEach(el=>new IntersectionObserver(es=>es.forEach(v=>{if(!v.isIntersecting||el.dataset.done)return;el.dataset.done="1";const end=Number(el.dataset.counter);let n=0;const t=setInterval(()=>{n=Math.min(end,n+Math.max(1,Math.ceil(end/18)));el.textContent=fa(n);if(n>=end)clearInterval(t)},45)}),{threshold:.6}).observe(el));
const slides=[...$$(".review-card")],dots=$("#reviewDots");slides.forEach((s,i)=>{const b=document.createElement("button");b.className=i?"":"active";b.onclick=()=>setSlide(i);dots.appendChild(b)});let slide=0;function setSlide(i){slide=i;slides.forEach((s,n)=>s.classList.toggle("active",n===i));[...dots.children].forEach((b,n)=>b.classList.toggle("active",n===i))}setInterval(()=>setSlide((slide+1)%slides.length),6500);
document.addEventListener("keydown",e=>e.key==="Escape"&&closeAll());
renderMenu();renderCart();updateCartCount();