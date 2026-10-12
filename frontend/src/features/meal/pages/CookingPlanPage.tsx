import { useState } from 'react'
import { Printer } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useDemoUser } from '../../../app/DemoUserContext'
import { Breadcrumbs, Button, Card, Field, Input, Notice, PageHeader, SectionHeader, Skeleton, StatusBadge } from '../../../components/ui'
import { useMealQuery } from '../hooks/useMealQuery'
import { cookingPlanApi, type CookingMealDemand } from '../services/cookingPlanApi'
import { dhakaToday } from '../services/menuApi'

function ItemDemand({ meal }: { meal: CookingMealDemand }) {
  return <Card><SectionHeader title={`${meal.mealType === 'LUNCH' ? 'Lunch' : 'Dinner'} individual items`} description="Final Year item orders that the kitchen should prepare separately." />
    {meal.finalYearItems.length ? <div className="responsive-table"><table><thead><tr><th>Menu item</th><th>Serving size</th><th className="numeric">Total to prepare</th></tr></thead><tbody>
      {meal.finalYearItems.map((item) => <tr key={item.itemId}><td>{item.name}</td><td>{item.menuQuantity || 'Not specified'}</td><td className="numeric"><strong>{item.orderedQuantity}</strong></td></tr>)}
    </tbody></table></div> : <p>No individual items ordered.</p>}
  </Card>
}

export default function CookingPlanPage() {
  const { user } = useDemoUser()
  const [date, setDate] = useState(() => dhakaToday())
  const { data, loading, error } = useMealQuery(() => cookingPlanApi.get(date, user.id), [date, user.id])
  const plan = data?.mealDate === date ? data : null
  const meals = plan ? [plan.breakfast, plan.lunch, plan.dinner] : []

  return <div className="page-enter wide-page">
    <Breadcrumbs items={['Meal Management', 'Cooking plan']} />
    <PageHeader title="Cooking plan" description="See how many portions the kitchen should prepare for the selected date." actions={<Button variant="secondary" icon={<Printer size={17} />} disabled={!plan} onClick={() => window.print()}>Print plan</Button>} />
    <div className="toolbar"><Field label="Meal date"><Input type="date" value={date} onChange={(event) => setDate(event.target.value)} /></Field></div>
    <Notice title="How the total is calculated">Years 1-4 students - approved absence notices + Final Year full-meal orders = total to prepare. Billing is calculated separately.</Notice>
    {error ? <Notice tone="error" title="Cooking plan unavailable">{error} <Link className="text-link" to="/meal/manage/menu">Create or publish a menu for this date</Link>.</Notice> : loading || data && !plan ? <Skeleton lines={7} /> : !plan ? <Notice tone="error" title="Cooking plan unavailable">Please try again.</Notice> : <>
      <div className="summary-strip"><span><b>{plan.breakfast.totalPackagesOrFullMeals}</b> breakfast packages to prepare</span><span><b>{plan.lunch.totalPackagesOrFullMeals}</b> lunch full meals to prepare</span><span><b>{plan.dinner.totalPackagesOrFullMeals}</b> dinner full meals to prepare</span></div>
      <Card className="table-card"><SectionHeader title="Kitchen quantities" description="Use the final column as the number of packages or full meals to prepare." />
        <div className="responsive-table"><table><thead><tr><th>Meal</th><th>Years 1-4 students</th><th>Approved absences</th><th>Regular portions</th><th>Final Year orders</th><th>Total to prepare</th></tr></thead><tbody>
          {meals.map((meal) => <tr key={meal.mealType}><td><strong>{meal.mealType[0] + meal.mealType.slice(1).toLowerCase()}</strong><small>{meal.menuDescription}</small><StatusBadge status={meal.available ? 'Available' : 'Unavailable'} /></td><td>{meal.yearsOneToFourBaseline}</td><td>{meal.reportedAbsent > 0 ? `-${meal.reportedAbsent}` : '0'}</td><td>{meal.yearsOneToFourPlanned}</td><td>{meal.finalYearPackagesOrFullMeals > 0 ? `+${meal.finalYearPackagesOrFullMeals}` : '0'}</td><td><strong>{meal.totalPackagesOrFullMeals}</strong></td></tr>)}
        </tbody></table></div></Card>
      <div className="detail-grid"><ItemDemand meal={plan.lunch} /><ItemDemand meal={plan.dinner} /></div>
    </>}
  </div>
}
